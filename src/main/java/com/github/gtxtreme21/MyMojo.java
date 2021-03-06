package com.github.gtxtreme21;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Goal which reads jtl files and creates a pass/fail report.
 *
 * @goal report
 * 
 * @phase test
 */
public class MyMojo extends AbstractMojo {
    /**
     * Location of the file.
     * @parameter default-value="${project.build.directory}/jmeter/results"
     * @required
     */
    private File inputDirectory;
    
    /**
     * Location of the file.
     * @parameter default-value="${project.build.directory}/jmeter/results/reports"
     * @required
     */
    private File outputDirectory;

	private JTLParser jtlParser = new JTLParser();

    public void execute() throws MojoExecutionException {
        File inputDir = inputDirectory;
        File outputDir = outputDirectory;

        if ( !outputDir.exists() ) {
            outputDir.mkdirs();
        }

        writeOutputFiles(inputDir, outputDir);
    }

	private void writeOutputFiles(File inputDir, File outputDir) throws MojoExecutionException {
		File resultHtml = new File( outputDir, "jmeter_fail_results.html" );

        FileWriter w2 = null;
        try {
        	String includeJsContent = getJsContent();
        	
        	
            w2 = new FileWriter( resultHtml );
            if (null != inputDir && inputDir.exists() && inputDir.isDirectory()) {
            	File[] jmeterResults = getJtlFiles(inputDir);
            	if (null != jmeterResults) {
            		w2.write( "<html><head><script type='text/javascript'>");
            		w2.write(includeJsContent);
            		w2.write("</script></head>");
            		w2.write("<body>found:"+jmeterResults.length+" jmeter results<div>" );
            		int nbrOfFailures = 0;
            		StringBuffer resultsSB = new StringBuffer();
            		resultsSB.append("<ul>");
            		for (File testFile: jmeterResults) {
            			TestResult results = jtlParser.getTestResult(testFile);
            			if(!results.isPassed()){
            				nbrOfFailures++;
            			}
            			resultsSB.append("\r\n<li>").append(results.toHtml()).append("</li>");
            		}
            		resultsSB.append("</ul>");
            		String resultAsString = resultsSB.toString();
            		//nbrOfFailures += StringUtils.countMatches(resultAsString, "(failed):");
        			if (0 < nbrOfFailures) {
        				w2.write("</ br><b><font size='3' color='red'>There are "+nbrOfFailures+" failures.</font></b>");
        				wrapFailuresInRedFont(resultAsString);
        			} else {
        				w2.write("</ br><b><font size='3' color='green'>There are "+nbrOfFailures+" failures.</font></b>");
        			}
        			w2.write(resultAsString + "</div></body></html>");
        			//System.out.println("There are "+nbrOfFailures+" failures.");
            	}
            } else {
            	w2.write( "<html>input directory not found</html>" );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error creating file " + resultHtml, e );
        }
        finally
        {
            if ( w2 != null )
            {
                try
                {
                    w2.close();
                }
                catch ( IOException e )
                {
                    // ignore
                }
            }            
        }
	}

	private String getJsContent() throws IOException {
		byte[] byteArray = new byte[1024];
		InputStream jsInclude = MyMojo.class.getClassLoader().getResourceAsStream("behavior.js");
    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
    	int read = 0;
    	while((read = jsInclude.read(byteArray)) > 0){
    		bos.write(byteArray, 0, read);
    	}
    	jsInclude.close();
		return new String(bos.toByteArray());
	}

	private void wrapFailuresInRedFont(String resultAsString) {
		resultAsString.replace("(failed):", "<b><font size='3' color='red'>(failed):</font></b>");
	}

	private File[] getJtlFiles(File dir) {
        return dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".jtl");
            }
        });
    }

}
