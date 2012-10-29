package org.dongq;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

/**
 * Hello world!
 * 
 */
public class App {
	public static void main(String[] args) {
		System.out.println("Hello World!");
		
		App app = new App();
		
		try {
			File gitDir = new File("test.git.repository");
			if(!gitDir.exists()) {
				System.out.println("create git dir");
				gitDir.mkdir();
			}
			app.createGitRepository(gitDir);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("done...");
	}

	public void createGitRepository(File gitDir) throws IOException {
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		builder.setGitDir(gitDir);
		builder.readEnvironment();// scan environment GIT_* variables
		builder.findGitDir();// scan up the file system tree

		Repository repository = builder.build();

		System.out.println(repository.getDirectory().getAbsolutePath());
	}
}
