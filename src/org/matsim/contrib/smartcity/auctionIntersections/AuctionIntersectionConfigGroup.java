package org.matsim.contrib.smartcity.auctionIntersections;

import org.matsim.core.config.ReflectiveConfigGroup;

public class AuctionIntersectionConfigGroup extends ReflectiveConfigGroup {

    public static final String GROUPNAME = "auctionIntersection";

    public static final String MANAGER_CLASS = "managerClass";

    private String managerClass;

    public AuctionIntersectionConfigGroup(){
        super(GROUPNAME);
    }

    public AuctionIntersectionConfigGroup(String name) {
        super(name);
    }

    public AuctionIntersectionConfigGroup(String name, boolean storeUnknownParametersAsStrings) {
        super(name, storeUnknownParametersAsStrings);
    }

    @StringSetter(MANAGER_CLASS)
    public void setWrapper(String wrapper) {
        this.managerClass = wrapper;
    }

    @StringGetter(MANAGER_CLASS)
    public String getWrapper() {
        return this.managerClass;
    }
}
