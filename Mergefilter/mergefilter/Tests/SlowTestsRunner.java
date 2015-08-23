package mergefilter.Tests;

import org.junit.experimental.categories.Categories;
import org.junit.experimental.categories.Categories.ExcludeCategory;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Categories.class)
@ExcludeCategory(FastTests.class)
@SuiteClasses(PrefixMergerTest.class)
public class SlowTestsRunner {

}
