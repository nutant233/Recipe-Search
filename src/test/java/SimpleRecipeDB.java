import com.fast.recipesearch.AbstractContainerRecipeDB;
import com.fast.recipesearch.IntLongMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.StreamSupport;

public class SimpleRecipeDB extends AbstractContainerRecipeDB<Recipe> {

    public SimpleRecipeDB(List<Runnable> branchBuilder) {
        super(branchBuilder);
    }

    public static Function<Recipe, Recipe> matchFunction(IntLongMap map) {
        return recipe -> {
            var container = recipe.container;
            if (container == null || container.match(map)) return recipe;
            return null;
        };
    }

    @Override
    protected IntLongMap extractIntMap(Recipe recipe) {
        return recipe.input;
    }

    /**
     * 主方法，演示配方数据库的创建、填充和搜索功能
     */
    public static void main(String[] args) {
        List<Recipe> recipes = new ArrayList<>();

        // 创建输入映射，定义搜索条件
        var input = new IntLongMap();
        // 添加多个键值对，每个键代表一种原料，值代表所需数量
        input.add(11, 10);
        input.add(22, 10);
        input.add(33, 10);
        input.add(44, 10);
        input.add(55, 10);
        input.add(66, 10);
        input.add(77, 10);
        input.add(88, 10);
        input.add(99, 10);
        input.add(12, 10);
        input.add(14, 10);
        input.add(16, 10);
        input.add(23, 10);
        input.add(43, 10);
        input.add(63, 10);
        input.add(83, 10);
        input.add(93, 10);
        input.add(111, 10);
        input.add(121, 10);
        input.add(131, 10);

        var ri = new IntLongMap();
        ri.add(33, 4);  // 添加原料33，需要4个单位
        ri.add(66, 2);  // 添加原料66，需要2个单位
        ri.add(99, 2);  // 添加原料99，需要2个单位
        var recipe = new Recipe(ri);
        recipes.add(recipe);

        ri = new IntLongMap();
        ri.add(22, 8);
        ri.add(55, 11);
        recipe = new Recipe(ri);
        recipes.add(recipe);

        ri = new IntLongMap();
        ri.add(22, 8);
        ri.add(55, 5);
        recipe = new Recipe(ri);
        recipes.add(recipe);

        ri = new IntLongMap();
        ri.add(11, 12);
        ri.add(44, 6);
        ri.add(77, 7);
        recipe = new Recipe(ri);
        recipes.add(recipe);

        ri = new IntLongMap();
        ri.add(11, 4);
        ri.add(44, 6);
        ri.add(77, 7);
        ri.add(88, 7);
        recipe = new Recipe(ri);
        recipes.add(recipe);

        ri = new IntLongMap();
        ri.add(11, 5);
        ri.add(22, 5);
        ri.add(88, 6);
        recipe = new Recipe(ri);
        recipes.add(recipe);

        ri = new IntLongMap();
        ri.add(22, 5);
        ri.add(88, 6);
        recipe = new Recipe(ri);
        recipes.add(recipe);

        ri = new IntLongMap();
        ri.add(11, 9);
        recipe = new Recipe(ri);
        recipes.add(recipe);

        ri = new IntLongMap();
        ri.add(22, 11);
        recipe = new Recipe(ri);
        recipes.add(recipe);

        ri = new IntLongMap();
        recipe = new Recipe(ri);
        recipes.add(recipe);

        var db = SimpleRecipeDB.create(recipes, SimpleRecipeDB::new);

        // 创建搜索迭代器，查找匹配输入条件的配方
        var ints = input.toIntArray();
        var mapFunction = matchFunction(input);

        // 查找并打印第一个匹配的配方
        System.out.println(db.findAnyMatch(input, ints, mapFunction));
        // 遍历并打印所有匹配的配方
        for (var r : db.fastSearch(input, ints, mapFunction)) {
            System.out.println(r);
        }

        // 并行测试搜索性能
        IntList list = new IntArrayList(1000000);
        for (int i = 0; i < 1000000; i++) {
            list.add(i);
        }
        var start = System.currentTimeMillis();
        var rs = list.intStream().parallel()
                .mapToObj(i -> db.search(input, ints, mapFunction))
                .flatMap(i -> StreamSupport.stream(i.spliterator(), false))
                .toList();
        db.clear();
        System.out.println(rs.size());
        System.out.println(System.currentTimeMillis() - start);
    }
}
