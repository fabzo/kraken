package fabzo.kraken.components;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class KubernetesComponent extends InfrastructureComponent {

    public static KubernetesComponent create() {
        return new KubernetesComponent();
    }

    protected KubernetesComponent() {

    }

    @Override
    public String name() {
        throw new NotImplementedException();
    }

}
