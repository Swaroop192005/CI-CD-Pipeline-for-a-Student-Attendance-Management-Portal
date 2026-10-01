// Create the Task 8 pipeline job as code.
//
// A Pipeline job whose definition comes from the Jenkinsfile in the repository,
// so the pipeline is versioned and reviewed with the application it builds.
import jenkins.model.*
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition
import hudson.plugins.git.*

def instance = Jenkins.getInstance()
def name = 'samp-pipeline'

if (instance.getItem(name) == null) {
    def job = instance.createProject(WorkflowJob, name)
    job.setDescription('Task 8 — pipeline as code. Checkout, build, test, package, ' +
                       'deploy to Tomcat and verify health. Defined by the Jenkinsfile in the repository.')

    def repoUrl = System.getenv('SAMP_REPO_URL') ?: '/workspace/samp'
    def branch  = System.getenv('SAMP_REPO_BRANCH') ?: '*/claude/determined-goldberg-ml3brm'
    def scm = new GitSCM(GitSCM.createRepoList(repoUrl, null), [new BranchSpec(branch)], null, null, [])

    job.setDefinition(new CpsScmFlowDefinition(scm, 'Jenkinsfile'))
    job.save()
    println "--> init: created pipeline job '${name}' from Jenkinsfile"
}
