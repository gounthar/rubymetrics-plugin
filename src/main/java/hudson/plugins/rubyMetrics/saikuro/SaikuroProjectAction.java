package hudson.plugins.rubyMetrics.saikuro;

import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.plugins.rubyMetrics.AbstractRubyMetricsProjectAction;

public class SaikuroProjectAction<SaikuroBuildAction> extends AbstractRubyMetricsProjectAction {

    public SaikuroProjectAction(Job<?, ?> job) {
        super(job);
    }

    @Deprecated
    public SaikuroProjectAction(AbstractProject<?, ?> project) {
        super(project);
    }

    public String getDisplayName() {
        return "Saikuro report";
    }

    public String getUrlName() {
        return "saikuro";
    }

    @Override
    protected Class getBuildActionClass() {
        return hudson.plugins.rubyMetrics.saikuro.SaikuroBuildAction.class;
    }

}
