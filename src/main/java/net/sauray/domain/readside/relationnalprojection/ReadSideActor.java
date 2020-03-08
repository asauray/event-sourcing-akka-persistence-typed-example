package net.sauray.domain.readside.relationnalprojection;

import akka.Done;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import net.sauray.application.Guardian.UpdateReadSides;

import java.sql.SQLException;


public class ReadSideActor extends AbstractBehavior<UpdateReadSides> {

    public static String readSideId = "my-read-side";

    public static Behavior<UpdateReadSides> create() {
        return Behaviors.setup(ReadSideActor::new);
    }

    private final ReadSideHandler readside;

    private ReadSideActor(ActorContext<UpdateReadSides> context) throws SQLException {
        super(context);

        Config conf = ConfigFactory.load();
        String host = conf.getString("slick.db.host");
        int port = conf.getInt("slick.db.port");
        String user = conf.getString("slick.db.user");
        String password = conf.getString("slick.db.password");
        String database = conf.getString("slick.db.database");

        readside = ReadSideHandler.init(user, password, database, host, port);
    }

    private Behavior<UpdateReadSides> update(UpdateReadSides wrapper) {
        try {
            readside.handleEvent(wrapper.persistenceId, wrapper.event);
            wrapper.replyTo.tell(Done.done());
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            return this;
        }
    }

    @Override
    public Receive<UpdateReadSides> createReceive() {
        return newReceiveBuilder()
                .onMessage(UpdateReadSides.class, this::update)
                .build();
    }
}
