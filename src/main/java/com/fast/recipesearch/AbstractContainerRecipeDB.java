package com.fast.recipesearch;

import java.util.List;

public abstract class AbstractContainerRecipeDB<R extends IntContainerHolder> extends AbstractRecipeDB<R> {

    public AbstractContainerRecipeDB(List<Runnable> branchBuilder) {
        super(branchBuilder);
    }

    @Override
    protected IntMapContainer getRecipeContainer(R recipe) {
        return recipe.getIntContainer();
    }

    @Override
    protected void setRecipeContainer(R recipe, IntMapContainer container) {
        recipeContainers.put(recipe, container);
        recipe.setIntContainer(container);
    }

    @Override
    public void build(List<Runnable> branchBuilder) {
        super.build(branchBuilder);
        recipeContainers = null;
    }
}
