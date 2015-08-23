package mergefilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PrefixMergerOld {

	public final static Logger logger = LogManager.getLogger(PrefixMergerOld.class.getName());
	
	public static void main(String[] args) {
/*		Scanner scanner = new Scanner(System.in);
		while (scanner.nextLine() != "\n") {
			int intervals[] = {16000, 16000}; //{1000, 2000, 4000, 8000, 16000, 32000, 64000};
			double seconds[] = {.61, 1.05, 3.5, 14, 58.5, 236, 902};
			for (int interval : intervals) {
				List<String> sources = new ArrayList<String>();
				List<String> prefixes = new ArrayList<String>();
				List<String> expected = new ArrayList<String>();
				long startime = System.currentTimeMillis();
				for(int i = 0; i<interval; i++) {
					char[] aaaa = new char[i];
					char[] bbbb = new char[i];
					Arrays.fill(aaaa, 'a');
					Arrays.fill(bbbb, 'b');
					sources.add(bbbb.toString());
					prefixes.add(aaaa.toString());
					expected.add(aaaa.toString());
				}
				long finTime = System.currentTimeMillis() - startime;
				PrefixMerger.logger.debug("time to fill arrays: " + String.valueOf(finTime/1000.0));
				
				long startTime = System.nanoTime();
				List<String> results = PrefixMerger.mergefilter(sources, prefixes);
				long elapsedTime = System.nanoTime() - startTime;
				
				//100 took .58 seconds, 1000 took .83, 10000 took 23 seconds, 20000 took 87 seconds
				PrefixMerger.logger.debug("sorting " + String.valueOf(interval) + " items took " 
						+ String.valueOf(elapsedTime/1000000000.0) + " seconds");
			}		
		}
		scanner.close();*/
	}
	/** Merges a list of prefixes and words that start with those prefixes. 
	 * @param sources 	must be sorted case-insensitively, no identical strings
	 * @param prefixes 	must be sorted case-insensitively, no identical strings
	 * @return 			sorted, merged list of words that (case-insensitively) start with prefixes, 
	 * 					and unused prefixes. 
	 */
	public static List<String> mergePrefixes(List<String> sources, List<String> prefixes) {
		
		if (prefixes.contains("")) {
			logger.warn("prefixes contains empty string, all sources will match");
		}
		
		LinkedHashMap<String, ArrayList<String>> prefixToSourceMap = new LinkedHashMap<String, ArrayList<String>>();
		for (String prefix : prefixes) {
			//keys will retain order in which we add them
			prefixToSourceMap.put(prefix, new ArrayList<String>());
		}

		for (String prefix : prefixes) {
			for ( String source : sources ) {
				if (source.toLowerCase().startsWith(prefix.toLowerCase())) {
					prefixToSourceMap.get(prefix).add(source);
				}
			}
		}
		
		List<String> results = mergeUniques(prefixToSourceMap);
		
		return results;
	}
	
	private static List<String> mergeUniques(LinkedHashMap<String, ArrayList<String>> prefixToSourceMap) {
		List<String> results = new ArrayList<String>(); 
		for (String prefix : prefixToSourceMap.keySet()) {
			ArrayList<String> sourcesList = prefixToSourceMap.get(prefix);
			if (sourcesList.isEmpty()) {
				results.add(prefix);
			} else {
				for (String source : sourcesList) {
					if (!results.contains(source)) {
						results.add(source);
					}
				}
			}
		}
		return results;
	}

}
