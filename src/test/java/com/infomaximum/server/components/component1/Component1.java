package com.infomaximum.server.components.component1;

import com.infomaximum.cluster.Cluster;
import com.infomaximum.cluster.struct.Component;
import com.infomaximum.cluster.struct.Info;

/**
 * Created by v.bukharkin on 19.05.2017.
 */
public class Component1 extends Component {

    public static final Info INFO = new Info.Builder<>("com.infomaximum.server.components.component1")
            .withComponentClass(Component1.class)
            .build();

    public Component1(Cluster cluster) {
        super(cluster);
    }

    @Override
    public Info getInfo() {
        return INFO;
    }

}
