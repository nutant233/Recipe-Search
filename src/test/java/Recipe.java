import com.fast.recipesearch.IntContainerHolder;
import com.fast.recipesearch.IntLongMap;
import com.fast.recipesearch.IntMapContainer;

public class Recipe implements IntContainerHolder {

    final IntLongMap input;
    IntMapContainer container;

    public Recipe(IntLongMap input) {
        this.input = input;
    }

    @Override
    public String toString() {
        return input.toString();
    }

    @Override
    public IntMapContainer getIntContainer() {
        return container;
    }

    @Override
    public void setIntContainer(IntMapContainer container) {
        this.container = container;
    }
}
