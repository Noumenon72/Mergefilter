package mergefilter.Tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import mergefilter.PrefixMerger;
	
	@RunWith(Parameterized.class)
	public class PrefixMergerTest {
	
	public final static Logger logger = LogManager.getLogger(PrefixMergerTest.class.getName());
	
	private List<String> _sources;
	private List<String> _prefixes;
	private List<String> _expected = new ArrayList<String>();
    private PrefixMerger mPrefixMerger;

	@Parameters
	public static List<String[][]> data() {
		List<String[][]> toBeTested = Arrays.asList(new String[][][]{
			{	/*Source list, prefix list, expected result*/ 
				{"", "adver@tised", "ha", "inbound", "intro", "introduce", "overwhelm", "Precious", "prepared"},
				{"@", "ham", "in", "inc", "intro", "over", "pre", "under"},
				{"@", "ham", "inbound", "inc", "intro", "introduce", "overwhelm", "Precious", "prepared", "under"}
			}, //tested: empty string in source, nonalphanumeric, source substring of prefix, prefix in sources, 
			//	prefix substring of other prefix, prefix needing to be between sources that matched a prior prefix (inc), 
			// capitalization, 0, 1, or 2 matches to one prefix, unmatched prefix at end
			{
				{"", "advertised", "introduce", "precarious", "Precious", "thiotimoline"},
				{"", "pre", "under"},
				{"", "advertised", "introduce", "precarious", "Precious", "thiotimoline", "under"}
			}, 		//tested: empty string in prefixes, unmatched source at end.
			{
				{""},
				{""},
				{""}
			}, 		//tested: only empty strings
			{
				{},
				{"", "pre", "under"},
				{"", "pre", "under"}
			},
			{
				{"", "advertised", "introduce", "Precious", "prepared", "thiotimoline"},
				{},
				{}
			},
			{
				{"NnQ", "NNXA", "NoFiXWXBDivOag", "oppenheimer"},
				{"N", "NnQ", "nobyrds", "op", "while"},
				{"NnQ", "NNXA", "nobyrds", "NoFiXWXBDivOag", "oppenheimer", "while" }
			}
		});
		
		return toBeTested;
	}

	public PrefixMergerTest(String[] sources, String[] prefixes, String[] expected) {
		_sources = Arrays.asList(sources);
		_prefixes = Arrays.asList(prefixes);
		_expected = new ArrayList<String>();
		_expected.addAll(Arrays.asList(expected));
		if (new HashSet<String>(_expected).size() > _expected.size()) {
			throw new IllegalArgumentException("There should be no duplicates in expected values");
		}
	}
	
	@Before
	public void setUp() throws Exception {
        mPrefixMerger = new PrefixMerger();
		assertTrue(true);
	}

	@Test
	public void testMergePrefixes() {
		logger.info("------------------testing main-------------");
		List<String> mergedFiltered = mPrefixMerger.mergePrefixes(_sources, _prefixes);

		StringBuilder errorMessage = new StringBuilder("Lists should be equal.\n ");
		if (!_expected.equals(mergedFiltered)) {
			List<String> extras = new ArrayList<String>(mergedFiltered);
			extras.removeAll(_expected);
			if (extras.size() > 0) {
				errorMessage.append("Result contains ")
				.append(extras.toString())
				.append( " when it should not.\n");				
			}
			List<String> missing = new ArrayList<String>(_expected);
			missing.removeAll(mergedFiltered);
			if (missing.size() > 0) {
				errorMessage.append("Result does not contain ")
				.append(missing.toString())
				.append("\n\n");				
			}
			HashSet<String> mergedFilterUniques = new HashSet<String>(mergedFiltered);
			if (mergedFilterUniques.size() < mergedFiltered.size()) {
				ArrayList<String> copy = new ArrayList<String>(mergedFiltered);	
				errorMessage.append("There are duplicate entries.\n")
				.append(CollectionUtils.subtract(copy, mergedFilterUniques))
				.append("\n");					
			}
			HashSet<String> expectedUniques = new HashSet<String>(_expected);
			if (!mergedFilterUniques.containsAll(expectedUniques)) {
				errorMessage.append("There are missing entries\n")
				.append(CollectionUtils.subtract(expectedUniques, mergedFilterUniques))
				.append(" not present.\n");
			}
		}
		assertEquals(errorMessage.toString(), _expected, mergedFiltered);
        if (errorMessage.getClass().equals("String")) {


        }
	}

}
