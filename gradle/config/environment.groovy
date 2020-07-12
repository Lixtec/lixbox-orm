desc_project{
    type="library"
    projectSite=false
    withDocker=true
    withRegistry=false
    withQuarkus=false
    version{
        majorVersion=8
        mediumVersion=1
        minorVersion=1
    }
    artefact{
        group="fr.lixbox.lixbox-orm"
        project="lixbox-orm"
        projectKey="${group}:${project}"
    }
	uri{
	   api_context="sans objet"
	   api="sans objet"
	   ui_context="sans objet"
	   ui="sans objet" 
	}
	pic{
	    channel="lixbox"
		git{
    	    uri="https://scm.service.lixtec.fr/${channel}/${desc_project.artefact.project}.git"
    	}    	
        jenkins{
            uri="https://ci.service.lixtec.fr/view/${channel}/job/${desc_project.artefact.project}-pipeline"
        }   
        sonar{
            uri="https://quality.service.lixtec.fr/dashboard?id=${desc_project.artefact.group}%3A${desc_project.artefact.project}"
        }
    }
}

artifactoryRepository{
	contextUrl="https://repos.service.lixtec.fr/artifactory"
    username="lixbox.jenkins.bot"
    password=".TL1b0sc!"
	lixboxRelease	{	
		name="lixbox-release"
	}
    lixboxSnapshot {
        name="lixbox-snapshot"
    }
    libsRelease{
		name="libs-release"
	}
}

sonarRepository{
	host{
		url="https://quality.service.lixtec.fr"
        username="lixbox.sonar.bot"
        password="@L1xb0x!"
	}
}