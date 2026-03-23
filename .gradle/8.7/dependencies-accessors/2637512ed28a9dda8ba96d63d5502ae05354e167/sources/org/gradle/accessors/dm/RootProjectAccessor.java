package org.gradle.accessors.dm;

import org.gradle.api.NonNullApi;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.internal.artifacts.dependencies.ProjectDependencyInternal;
import org.gradle.api.internal.artifacts.DefaultProjectDependencyFactory;
import org.gradle.api.internal.artifacts.dsl.dependencies.ProjectFinder;
import org.gradle.api.internal.catalog.DelegatingProjectDependency;
import org.gradle.api.internal.catalog.TypeSafeProjectDependencyFactory;
import javax.inject.Inject;

@NonNullApi
public class RootProjectAccessor extends TypeSafeProjectDependencyFactory {


    @Inject
    public RootProjectAccessor(DefaultProjectDependencyFactory factory, ProjectFinder finder) {
        super(factory, finder);
    }

    /**
     * Creates a project dependency on the project at path ":"
     */
    public SpringSagaProjectDependency getSpringSaga() { return new SpringSagaProjectDependency(getFactory(), create(":")); }

    /**
     * Creates a project dependency on the project at path ":CommonService"
     */
    public CommonServiceProjectDependency getCommonService() { return new CommonServiceProjectDependency(getFactory(), create(":CommonService")); }

    /**
     * Creates a project dependency on the project at path ":OrderService"
     */
    public OrderServiceProjectDependency getOrderService() { return new OrderServiceProjectDependency(getFactory(), create(":OrderService")); }

    /**
     * Creates a project dependency on the project at path ":PaymentService"
     */
    public PaymentServiceProjectDependency getPaymentService() { return new PaymentServiceProjectDependency(getFactory(), create(":PaymentService")); }

    /**
     * Creates a project dependency on the project at path ":ProductService"
     */
    public ProductServiceProjectDependency getProductService() { return new ProductServiceProjectDependency(getFactory(), create(":ProductService")); }

    /**
     * Creates a project dependency on the project at path ":ShipmentService"
     */
    public ShipmentServiceProjectDependency getShipmentService() { return new ShipmentServiceProjectDependency(getFactory(), create(":ShipmentService")); }

    /**
     * Creates a project dependency on the project at path ":UserService"
     */
    public UserServiceProjectDependency getUserService() { return new UserServiceProjectDependency(getFactory(), create(":UserService")); }

}
