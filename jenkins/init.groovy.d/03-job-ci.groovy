// Create the Task 7 continuous-integration job as code.
//
// A freestyle Maven build over the project's Git repository, polling SCM, that
// archives the produced WAR. Defined here rather than clicked together in the UI
// so the job survives a rebuild of the container and is reviewable in the repo.
import jenkins.model.*
import hudson.model.FreeStyleProject
import hudson.plugins.git.*
import hudson.tasks.*
import hudson.triggers.SCMTrigger
import hudson.util.DescribableList

def instance = Jenkins.getInstance()
def name = 'samp-ci-build'

if (instance.getItem(name) == null) {
    def job = instance.createProject(FreeStyleProject, name)
    job.setDescription('Task 7 — CI build for the Student Attendance Management Portal. ' +
                       'Compiles, runs the unit tests and archives the deployable WAR.')

    def repoUrl = System.getenv('SAMP_REPO_URL') ?: '/workspace/samp'
    def branch  = System.getenv('SAMP_REPO_BRANCH') ?: '*/claude/determined-goldberg-ml3brm'
    def scm = new GitSCM(
        GitSCM.createRepoList(repoUrl, null),
        [new BranchSpec(branch)],
        null, null, [])
    job.setScm(scm)

    // Poll every 5 minutes; a commit therefore starts a build without manual action.
    job.addTrigger(new SCMTrigger('H/5 * * * *'))

    job.getBuildersList().add(new Shell(
        'export JAVA_HOME=/opt/java/openjdk\n' +
        'export PATH="$JAVA_HOME/bin:/opt/maven/bin:$PATH"\n' +
        'java -version\n' +
        'mvn -B -Dmaven.repo.local=/var/maven-cache/repository clean package\n'))

    job.getPublishersList().add(new ArtifactArchiver('target/*.war', '', false, true))
    job.getPublishersList().add(new hudson.tasks.junit.JUnitResultArchiver('target/surefire-reports/*.xml'))

    job.save()
    println "--> init: created freestyle CI job '${name}'"
}
