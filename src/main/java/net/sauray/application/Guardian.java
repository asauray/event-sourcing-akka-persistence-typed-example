package net.sauray.application;

import akka.Done;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import net.sauray.application.Guardian.CommandWrapper;
import net.sauray.domain.BankAccountEntity;
import net.sauray.domain.commands.BankCommand;
import net.sauray.domain.events.BankEvent;
import net.sauray.domain.readside.relationnalprojection.ReadSideActor;

import java.util.HashMap;
import java.util.Map;

public class Guardian extends AbstractBehavior<CommandWrapper> {

    interface CommandWrapper {

    }

    public static class BankCommandWrapper implements CommandWrapper {
        private BankCommand cmd;

        public BankCommandWrapper(BankCommand cmd) {
            this.cmd = cmd;
        }
    }

    public static class ChildTerminated implements CommandWrapper {
        private String entityId;

        public ChildTerminated(String entityId) {
            this.entityId = entityId;
        }
    }

    public static class UpdateReadSides implements CommandWrapper {

        public final BankEvent event;
        public final String persistenceId;

        public final ActorRef<Done> replyTo;

        public UpdateReadSides(String persistenceId, BankEvent event, ActorRef<Done> replyTo) {
            this.event = event;
            this.persistenceId = persistenceId;
            this.replyTo = replyTo;
        }
    }

    public static Behavior<CommandWrapper> create() {
        return Behaviors.setup(Guardian::new);
    }
    private Map<String, ActorRef<BankCommand>> children = new HashMap<>();

    private ActorRef<UpdateReadSides> readSide1;

    private Guardian(ActorContext<CommandWrapper> context) {
        super(context);
        readSide1 = getContext().spawn(ReadSideActor.create(), ReadSideActor.readSideId);
        getContext().watchWith(readSide1, new ChildTerminated(ReadSideActor.readSideId));
    }

    private Behavior<CommandWrapper> updateReadSides(UpdateReadSides wrapper) {
        readSide1.tell(wrapper);
        return this;
    }

    @Override
    public Receive<CommandWrapper> createReceive() {
        return newReceiveBuilder()
                .onMessage(BankCommandWrapper.class, this::onDelegateToChild)
                .onMessage(ChildTerminated.class, this::onChildTerminated)
                .onMessage(UpdateReadSides.class, this::updateReadSides)
                .build();
    }

    private Behavior<CommandWrapper> onDelegateToChild(BankCommandWrapper wrapper) {
        ActorRef<BankCommand> ref = children.get(wrapper.cmd.entityId());
        if (ref == null) {
            ref = getContext().spawn(BankAccountEntity.create(wrapper.cmd.entityId()), wrapper.cmd.entityId());
            getContext().watchWith(ref, new ChildTerminated(wrapper.cmd.entityId()));
            children.put(wrapper.cmd.entityId(), ref);
        }
        ref.tell(wrapper.cmd);
        return this;
    }

    private Behavior<CommandWrapper> onChildTerminated(ChildTerminated command) {
        children.remove(command.entityId);
        return this;
    }
}
