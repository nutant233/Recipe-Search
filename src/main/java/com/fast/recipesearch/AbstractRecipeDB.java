package com.fast.recipesearch;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;

import java.util.*;
import java.util.function.Function;

public abstract class AbstractRecipeDB<R> {

    public static <R, DB extends AbstractRecipeDB<R>> DB create(Collection<R> recipes, Function<List<Runnable>, DB> databaseBuilder) {
        List<Runnable> branchConstructionTasks = new ArrayList<>(recipes.size());
        Reference2ReferenceOpenHashMap<R, IntLongMap> recipeIntMaps = new Reference2ReferenceOpenHashMap<>(recipes.size());
        Int2IntOpenHashMap frequencyMap = new Int2IntOpenHashMap();
        DB database = databaseBuilder.apply(branchConstructionTasks);
        recipes.forEach(recipe -> database.collectRecipeData(frequencyMap, recipeIntMaps, recipe));
        recipeIntMaps.forEach((recipe, intMap) -> database.reorderRecipeByFrequency(frequencyMap, recipe, intMap));
        database.build(branchConstructionTasks);
        if (branchConstructionTasks.size() > 1000) {
            branchConstructionTasks.parallelStream().forEach(Runnable::run);
        } else {
            branchConstructionTasks.forEach(Runnable::run);
        }
        return database;
    }

    protected Branch<R> rootBranch = Branch.create();
    protected Map<R, IntMapContainer> recipeContainers = new Reference2ReferenceOpenHashMap<>();
    protected List<R> serialRecipes = new ArrayList<>();
    protected List<R> parallelRecipes = new ArrayList<>();
    protected RecipeSearcher<R> searchContext = new RecipeSearcher<>();
    protected int maxSearchDepth;
    protected int minParallelThreshold = 100;

    public AbstractRecipeDB(List<Runnable> branchConstructionTasks) {
        branchConstructionTasks.add(() -> rootBranch = rootBranch.optimize());
    }

    protected abstract IntLongMap extractIntMap(R recipe);

    protected boolean supportsParallel(R recipe) {
        return true;
    }

    protected IntMapContainer getRecipeContainer(R recipe) {
        return recipeContainers.get(recipe);
    }

    protected void setRecipeContainer(R recipe, IntMapContainer container) {
        recipeContainers.put(recipe, container);
    }

    public R findAnyMatch(IntLongMap map, int[] searchKeys, Function<R, R> recipeProcessor) {
        R foundRecipe = new RecipeSearcher<>(maxSearchDepth, rootBranch, map, searchKeys, recipeProcessor, null).findAny();
        if (foundRecipe != null) return foundRecipe;
        if (!parallelRecipes.isEmpty()) {
            foundRecipe = findInParallel(parallelRecipes, recipeProcessor);
            if (foundRecipe != null) return foundRecipe;
        }
        if (!serialRecipes.isEmpty()) {
            return findInSerial(serialRecipes, recipeProcessor);
        }
        return null;
    }

    protected static <R> R findInParallel(List<R> recipes, Function<R, R> recipeProcessor) {
        return recipes.parallelStream().map(recipeProcessor).filter(Objects::nonNull).findAny().orElse(null);
    }

    protected static <R> R findInSerial(List<R> recipes, Function<R, R> recipeProcessor) {
        for (R recipe : recipes) {
            R processedRecipe = recipeProcessor.apply(recipe);
            if (processedRecipe != null) return processedRecipe;
        }
        return null;
    }

    public Iterator<R> createFallbackIterator(Function<R, R> recipeProcessor) {
        Iterator<R> parallelIterator = parallelRecipes.isEmpty() ? null : parallelRecipes.parallelStream().map(recipeProcessor).filter(Objects::nonNull).iterator();
        Iterator<R> serialIterator = serialRecipes.isEmpty() ? null : IteratorUtil.map(serialRecipes.iterator(), recipeProcessor);
        if (parallelIterator == null) return serialIterator;
        if (serialIterator == null) return parallelIterator;
        return IteratorUtil.concat(parallelIterator, serialIterator);
    }

    public Iterable<R> searchFallback(Function<R, R> recipeProcessor) {
        Iterator<R> iterator = createFallbackIterator(recipeProcessor);
        return iterator == null ? Collections.emptyList() : IteratorUtil.wrap(iterator);
    }

    public Iterable<R> search(IntLongMap map, int[] searchKeys, Function<R, R> recipeProcessor) {
        return new RecipeSearcher<>(maxSearchDepth, rootBranch, map, searchKeys, recipeProcessor, createFallbackIterator(recipeProcessor));
    }

    public Iterable<R> fastSearch(IntLongMap map, int[] searchKeys, Function<R, R> recipeProcessor) {
        searchContext.reset(rootBranch, map, searchKeys, recipeProcessor, createFallbackIterator(recipeProcessor));
        return searchContext;
    }

    protected void build(List<Runnable> branchConstructionTasks) {
        recipeContainers.forEach((recipe, container) -> addToBranch(branchConstructionTasks, recipe, container));
        if (parallelRecipes.size() < minParallelThreshold) {
            serialRecipes.addAll(parallelRecipes);
            parallelRecipes = Collections.emptyList();
        }
        if (serialRecipes.isEmpty()) {
            serialRecipes = Collections.emptyList();
        }
    }

    protected void addToBranch(List<Runnable> branchConstructionTasks, R recipe, IntMapContainer container) {
        int[] keys = container.key;
        int searchDepth = Math.min(64, keys.length);
        maxSearchDepth = Math.max(maxSearchDepth, searchDepth);
        addToBranch(branchConstructionTasks, recipe, searchDepth, keys, rootBranch);
    }

    private void addToBranch(List<Runnable> branchConstructionTasks, R recipe, int depth, int[] keys, Branch<R> branch) {
        Branch<R> currentBranch = branch;
        int lastIndex = depth - 1;
        for (int i = 0; i < depth; i++) {
            boolean isIntermediateNode = i < lastIndex;
            Node<R> node = ((Branch.HashBranch<R>) currentBranch).compute(keys[i], (key, existingNode) -> isIntermediateNode ? Node.branch(branchConstructionTasks, existingNode) : Node.recipe(branchConstructionTasks, existingNode, recipe));
            if (isIntermediateNode) {
                currentBranch = ((Node.BranchNode<R>) node).branch();
            }
        }
    }

    protected void collectRecipeData(Int2IntOpenHashMap frequencyMap, Map<R, IntLongMap> recipeIntMaps, R recipe) {
        if (recipe == null) return;
        IntLongMap intMap = extractIntMap(recipe);
        int[] keys = intMap.toIntArray();
        if (keys.length == 0) {
            if (supportsParallel(recipe)) {
                parallelRecipes.add(recipe);
            } else {
                serialRecipes.add(recipe);
            }
        } else {
            for (var key : keys) frequencyMap.addTo(key, 1);
            setRecipeContainer(recipe, new IntMapContainer(keys));
            recipeIntMaps.put(recipe, intMap);
        }
    }

    protected void reorderRecipeByFrequency(Int2IntOpenHashMap frequencyMap, R recipe, IntLongMap intMap) {
        IntMapContainer container = getRecipeContainer(recipe);
        int[] keys = container.key;
        IntArrays.stableSort(keys, (a, b) -> Integer.compare(frequencyMap.get(a), frequencyMap.get(b)));
        int keyCount = keys.length;
        long[] values = new long[keyCount];
        for (int i = 0; i < keyCount; i++) {
            values[i] = intMap.get(keys[i]);
        }
        container.value = values;
    }

    public void clear() {
        rootBranch = Branch.create();
        recipeContainers = new Reference2ReferenceOpenHashMap<>();
        serialRecipes = new ArrayList<>();
        parallelRecipes = new ArrayList<>();
        searchContext = new RecipeSearcher<>();
        maxSearchDepth = 0;
    }
}
