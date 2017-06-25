package fabzo.kraken.components;


import fabzo.kraken.wait.Wait;
import io.vavr.collection.List;

public abstract class InfrastructureComponent implements Named {
    private ComponentState state = ComponentState.WAITING;
    private List<Wait> waitFunctions = List.empty();

    public InfrastructureComponent withWait(final Wait waitFunc) {
        this.waitFunctions = waitFunctions.append(waitFunc);
        return this;
    }

    public List<Wait> waitFuncs() {
        return waitFunctions;
    }

    public void setState(final ComponentState state) {
        this.state = state;
    }

    public ComponentState state() {
        return state;
    }
}