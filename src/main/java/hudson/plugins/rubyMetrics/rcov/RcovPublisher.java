package hudson.plugins.rubyMetrics.rcov;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.plugins.rubyMetrics.HtmlPublisher;
import hudson.plugins.rubyMetrics.rcov.model.MetricTarget;
import hudson.plugins.rubyMetrics.rcov.model.RcovResult;
import hudson.plugins.rubyMetrics.rcov.model.Targets;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.apache.commons.beanutils.ConvertUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Rcov {@link Publisher}
 *
 * @author David Calavera
 *
 */
@SuppressWarnings({"unchecked", "serial"})
public class RcovPublisher extends HtmlPublisher implements SimpleBuildStep {

    private List<MetricTarget> targets = new ArrayList<MetricTarget>(){{
        add(new MetricTarget(Targets.TOTAL_COVERAGE, 80, null, null));
        add(new MetricTarget(Targets.CODE_COVERAGE, 80, null, null));
    }};

    @DataBoundConstructor
    public RcovPublisher(String reportDir) {
        this.reportDir = reportDir;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull Launcher launcher,
                        @NonNull TaskListener listener) throws InterruptedException, IOException {
        final RcovFilenameFilter indexFilter = new RcovFilenameFilter();
        prepareMetricsReportBeforeParse(run, workspace, listener, indexFilter, DESCRIPTOR.getToolShortName());
        if (run.getResult() == Result.FAILURE) {
            return;
        }

        RcovParser parser = new RcovParser(run.getRootDir());
        RcovResult results = parser.parse(getCoverageFiles(run, indexFilter)[0]);

        RcovBuildAction action = new RcovBuildAction(run, results, targets);
        run.getActions().add(action);

        if (failMetrics(results, listener)) {
            run.setResult(Result.UNSTABLE);
        }
    }

    private boolean failMetrics(RcovResult results, TaskListener listener) {
        float initRatio = 0;
        float resultRatio = 0;
        for (MetricTarget target : targets) {
            initRatio = target.getUnstable();
            resultRatio = results.getRatioFloat(target.getMetric());

            if (resultRatio < initRatio) {
                listener.getLogger().println("Code coverage enforcement failed for the following metrics:");
                listener.getLogger().println("    " + target.getMetric().getName());
                return true;
            }
        }
        return false;
    }

    public List<MetricTarget> getTargets() {
        return targets;
    }

    @DataBoundSetter
    public void setTargets(List<MetricTarget> targets) {
        this.targets = targets;
    }

    /**
     * Descriptor should be singleton.
     */
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        private final List<MetricTarget> targets;

        protected DescriptorImpl() {
            super(RcovPublisher.class);
            targets = new ArrayList<MetricTarget>(){{
                add(new MetricTarget(Targets.TOTAL_COVERAGE, 80, null, null));
                add(new MetricTarget(Targets.CODE_COVERAGE, 80, null, null));
            }};
        }

        public String getToolShortName() {
            return "rcov";
        }

        @Override
        public String getDisplayName() {
            return "Publish Rcov report";
        }

        public List<MetricTarget> getTargets(RcovPublisher instance) {
            return instance != null && instance.getTargets() != null?instance.getTargets() : getDefaultTargets();
        }

        private List<MetricTarget> getDefaultTargets() {
            return targets;
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public RcovPublisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            RcovPublisher instance = req.bindParameters(RcovPublisher.class, "rcov.");

            ConvertUtils.register(MetricTarget.CONVERTER, Targets.class);
            List<MetricTarget> targets = req.bindParametersToList(MetricTarget.class, "rcov.target.");
            instance.setTargets(targets);
            return instance;
        }

    }

    @Override
    public BuildStepDescriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
    }

    private static class RcovFilenameFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return name.equalsIgnoreCase("index.html");
        }
    }

}
