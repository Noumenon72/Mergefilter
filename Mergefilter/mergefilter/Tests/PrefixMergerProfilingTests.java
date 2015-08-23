package mergefilter.Tests;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import mergefilter.PrefixMerger;
	
	public class PrefixMergerProfilingTests {
	
	public final static Logger logger = LogManager.getLogger(PrefixMergerProfilingTests.class.getName());
	
    private PrefixMerger mPrefixMerger;
	
	@Before
	public void setUp() throws Exception {
        mPrefixMerger = new PrefixMerger();
	}

	@Category(SlowTests.class)
	@Test
	public void testRuntimeWithRandomStrings() throws IOException {
        //        int sizes[] = { 4000 };
        //        int sizes[] = { 32000 };
        int sizes[] = { 4000, 8000, 16000, 32000, 64000, 128000 };
        // 4000 8000 16000 32000 64000 128000
	//oldmergetimes:  { 3.2, 12,  48+64 to fill arrays, 206+206 to fill arrays, --, --
		//mergetimes: { na ,  .17,    .34,    2,    16,    128} = O(n^3)?! for new class
//after moving toLoer: {.18,.85,      5.4,   47,   471,
//after moving back:   	.23, 1.6, 10.3,      77,
//6:34 8/18				.28  1.7  10.8      96,    996,   2979... probably turned it off? 
//											87
//											15?!
        // 9
        // ___________________.18 .34__ .66_____ 2.7____ 17 _____80
        // logoff _____________.08 .11__.31____1.8______14.3______80
        // collate takes ____2.8, 3.2, 4.9,___9.1______33________138
        // now collate takes 2.3, 9.2, 38, 156...
        //                    1.8,6.8,28.5,108
		for (int size : sizes) {
		    long startime = System.currentTimeMillis();
            List<String> sources = new ArrayList<String>(size);
            List<String> prefixes = new ArrayList<String>(size);
            Path path = Paths.get("./mergefilter/Tests").toAbsolutePath();
            String sourceFilename = String.valueOf(size) + "randomsources.txt";
            String prefixFilename = String.valueOf(size) + "randomprefixes.txt";

            File sourcefile = new File(path.resolve(sourceFilename).toString());
            FileReader fr = new FileReader(sourcefile);
            BufferedReader br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null) {
                sources.add(line);
            }
            br.close();
            fr.close();

            File prefixfile = new File(path.resolve(prefixFilename).toString());
            FileReader fr2 = new FileReader(prefixfile);
            BufferedReader br2 = new BufferedReader(fr2, 8192);
            String line2;
            while ((line2 = br2.readLine()) != null) {
                prefixes.add(line2);
            }
            br2.close();
            fr2.close();

			/*try (Stream<String> sourcelines = Files.lines(path.resolve(sourceFilename));
					Stream<String> prefixlines = Files.lines(path.resolve(prefixFilename))) {
				sources = sourcelines
						.onClose(() -> System.out.println("File closed"))
						.collect(Collectors.toList());
				// took 53 sec to fill 64000 .forEachOrdered(sources::add); now 89 -- i think I'm out of memory
				 * now 112 with the bufferedreader, now 145... after a reboot, 109, 116,
				 * now it's 3.8??? back to 109, and 1105 for 128000
				prefixes = prefixlines.collect(Collectors.toList());
			} */
            long finTime = System.currentTimeMillis() - startime;

            if (sources.size() != size) {
                logger.error("sources size is only {}, and should have been {}", String.valueOf(sources.size()),
                        String.valueOf(size));
            }
            if (prefixes.size() != size) {
                logger.error("prefixes size is only {}, and should have been {}", String.valueOf(prefixes.size()),
                        String.valueOf(size));
            }
            assert sources.size() == size;
            assert prefixes.size() == size;

            logger.warn("time to fill arrays: " + String.valueOf(finTime / 1000.0));

            long startTime = System.nanoTime();
            mPrefixMerger.mergePrefixes(sources, prefixes);
            long elapsedTime = System.nanoTime() - startTime;
			
            logger.warn("merging " + String.valueOf(sources.size()) + " random strings took "
                    + String.valueOf(elapsedTime / 1000000000.0) + " seconds");
			
		}
	}
	
	@Category(SlowTests.class)
	@Test
	public void testRuntimeWithLargeArrays() {
        //        int intervals[] = { 32000 };
        int intervals[] = { 4000, 8000, 16000, 32000, 64000, 128000 };
		//seconds={.61, 1.05, 3.5, 14,  58.5, 236,   902,      --?};
		//8/18 with old:   {2.8,   10.5, 43, 173,    667, --}
		//newSeconds =     [.014, .26, .37, 1.99, 5.36, 29.7, 91, 128000=449]
        // ___________________________.46,_ 1.77, 5.57, 44.3, 89
        // faster 128000 takes .099? 8 with logging turned on.
        // collate takes 9s for 4000, 47 for 8000
        // now collate is { .26, .29, .83, 3.15,      13.4,     59  with logging on    
		for (int interval : intervals) {
			List<String> sources = new ArrayList<String>();
			List<String> prefixes = new ArrayList<String>();
			List<String> expected = new ArrayList<String>();
			long startime = System.currentTimeMillis();
			for(int i = 0; i<interval; i++) {
				char[] aaaa = new char[i];
				Arrays.fill(aaaa, 'a');
				sources.add(aaaa.toString());
				prefixes.add(aaaa.toString());
				expected.add(aaaa.toString());
			}
			long finTime = System.currentTimeMillis() - startime;
            logger.warn("time to fill arrays: " + String.valueOf(finTime / 1000.0));
			
			long startTime = System.nanoTime();
			mPrefixMerger.mergePrefixes(sources, prefixes);
			long elapsedTime = System.nanoTime() - startTime;
			
            logger.warn("sorting " + String.valueOf(interval) + " large arrays took "
					+ String.valueOf(elapsedTime/1000000000.0) + " seconds");			
		}
	}
	


}
