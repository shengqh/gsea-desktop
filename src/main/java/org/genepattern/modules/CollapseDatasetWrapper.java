/*
 *  Copyright (c) 2003-2019 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California.  All rights reserved.
 */
package org.genepattern.modules;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import edu.mit.broad.genome.Conf;
import xtools.api.AbstractTool;
import xtools.munge.CollapseDataset;

public class CollapseDatasetWrapper extends AbstractModule {
    private static final Logger klog = Logger.getLogger(CollapseDatasetWrapper.class);
    
    // Suppressing the static-access warnings because this is the recommended usage according to the Commons-CLI docs.
    @SuppressWarnings("static-access")
    private static Options setupCliOptions() {
        Options options = new Options();
        options.addOption(OptionBuilder.withArgName("expressionDataset").hasArg().create("res"));
        options.addOption(OptionBuilder.withArgName("chipPlatform").hasArg().create("chip"));
        options.addOption(OptionBuilder.withArgName("collapseMode").hasArg().create("mode"));
        options.addOption(OptionBuilder.withArgName("omitFeaturesWithNoSymbolMatch").hasArg().create("include_only_symbols"));
        options.addOption(OptionBuilder.withArgName("outFile").hasArg().create("out"));
        options.addOption(OptionBuilder.withArgName("reportLabel").hasArg().create("rpt_label"));
        options.addOption(OptionBuilder.withArgName("devMode").hasArg().create("dev_mode"));
        options.addOption(OptionBuilder.withArgName("gpModuleMode").hasArg().create("run_as_genepattern"));
        return options;
    }

    public static void main(final String[] args) throws Exception {
        // Success flag. We set this to *false* until proven otherwise by a successful Tool run. This saves having to catch
        // all manner of exceptions along the way; just allow them to propagate to the top-level handler.
        boolean success = false;

        AbstractTool tool = null;

        File analysis = null;
        File tmp_working = null;
        File cwd = null;
        try {
            Options opts = setupCliOptions();
            CommandLineParser parser = new PosixParser();
            CommandLine cl = parser.parse(opts, args);

            // We want to check *all* params before reporting any errors so that the user sees everything that went wrong.
            boolean paramProcessingError = false;

            // Properties object to gather parameter settings to be passed to the Tool
            Properties paramProps = new Properties();

            // The GP modules should declare they are running in GP mode.  This has minor effects on the error messages
            // and runtime behavior.
            boolean gpMode = StringUtils.equalsIgnoreCase(cl.getOptionValue("run_as_genepattern"), "true");

            if (gpMode) {
                // Turn off debugging in the GSEA code and tell it not to create directories
                // TODO: confirm the "mkdir" property works as expected
                System.setProperty("debug", "false");
                System.setProperty("mkdir", "false");

                // Set the GSEA update check String to show this is coming from the modules.
                System.setProperty("UPDATE_CHECK_EXTRA_PROJECT_INFO", "GP_MODULES");

                String outOption = cl.getOptionValue("out");
                if (StringUtils.isNotBlank(outOption)) {
                    klog.warn("-out parameter ignored; only valid wih -run_as_genepattern false.");
                }
    
                // Define a working directory, to be cleaned up on exit. The name starts with a '.' so it's hidden from GP & file system.
                // Also, define a dedicated directory for building the report output
                cwd = new File(System.getProperty("user.dir"));
                tmp_working = new File(".tmp_gsea");
                tmp_working.mkdirs();

                // Define a working directory, to be cleaned up on exit. The name starts with a '.' so it's hidden from GP & file system.
                // Also, define a dedicated directory for building the report output
                cwd = new File(System.getProperty("user.dir"));
                tmp_working = new File(".tmp_gsea");
                analysis = new File(tmp_working, "analysis");
                analysis.mkdirs();
            } else {
                // Set the GSEA update check String to show this is CLI usage.
                System.setProperty("UPDATE_CHECK_EXTRA_PROJECT_INFO", "GSEA_CLI");
            }

            // Enable any developer-only settings. For now, this just disables the update check; may do more in the future
            boolean devMode = StringUtils.equalsIgnoreCase(cl.getOptionValue("dev_mode"), "true");
            if (devMode) {
                System.setProperty("MAKE_GSEA_UPDATE_CHECK", "false");
            }

            String expressionDataFileName = cl.getOptionValue("res");
            if (StringUtils.isNotBlank(expressionDataFileName)) {
                if (gpMode) {
                    expressionDataFileName = copyFileWithoutBadChars(expressionDataFileName, tmp_working);
                    paramProcessingError |= (expressionDataFileName == null);
                }
            } else {
                String paramName = (gpMode) ? "expression.dataset" : "-res";
                klog.error("Required parameter '" + paramName + "' not found.");
                paramProcessingError = true;
            }

            String chipPlatformFileName = cl.getOptionValue("chip");
            if (StringUtils.isNotBlank(chipPlatformFileName)) {
                if (gpMode) {
                    chipPlatformFileName = copyFileWithoutBadChars(chipPlatformFileName, tmp_working);
                    paramProcessingError |= (chipPlatformFileName == null);
                }
            } else {
                String paramName = (gpMode) ? "chip.platform.file" : "-chip";
                klog.error("Required parameter '" + paramName + "' not found");
                paramProcessingError = true;
            }

            String rptLabel = cl.getOptionValue("rpt_label");
            if (StringUtils.isBlank(rptLabel)) {
                rptLabel = "my_analysis";
            }

            if (paramProcessingError) {
                // Should probably use BadParamException and set an errorCode, use it to look up a Wiki Help page.
                throw new Exception("There were one or more errors with the job parameters.  Please check log output for details.");
            }

            klog.info("Parameters passing to CollapseDataset.main:");
            setParam("res", expressionDataFileName, paramProps, klog);
            setParam("chip", chipPlatformFileName, paramProps, klog);
            setParam("rpt_label", rptLabel, paramProps, klog);
            setParam("gui", "false", paramProps, klog);
            if (gpMode) {
                setParam("out", analysis.getPath(), paramProps, klog);
            } else {
                // For regular CLI mode just pass through -out instead of setting tmpdir
                setOptionValueAsParam("out", cl, paramProps, klog);
            }

            // Finally, load up the remaining simple parameters. We'll let GSEA validate these.
            setOptionValueAsParam("include_only_symbols", cl, paramProps, klog);
            setOptionValueAsParam("mode", cl, paramProps, klog);

            tool = new CollapseDataset(paramProps);
            try {
                success = AbstractTool.module_main(tool);
            } finally {
                try {
                    if (!analysis.exists()) return;
                    copyAnalysisToCurrentDir(cwd, analysis, false, null);
                } catch (IOException ioe) {
                    System.err.println("Error during clean-up:");
                    throw ioe;
                }
            }
        } catch (Throwable t) {
            success = false;
            klog.error("Error while processng:");
            klog.error(t.getMessage());
            t.printStackTrace(System.err);
        } finally {
            try {
                if (cwd != null && tmp_working != null) {
                    klog.info("clean");
                    cleanUpAnalysisDirs(cwd, tmp_working);
                }
            } finally {
                Conf.exitSystem(!success);
            }
        }
    }
}
