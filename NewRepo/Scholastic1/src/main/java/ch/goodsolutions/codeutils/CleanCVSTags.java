package ch.goodsolutions.codeutils;

import java.io.File;

import org.olat.core.util.FileUtils;
import org.olat.core.util.FileVisitor;

public class CleanCVSTags {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		// work in progress
		final FileVisitor fv = new FileVisitor() {

			@Override
			public void visit(final File file) {
				// TODO Auto-generated method stub
				final String fname = file.getName();
				if (fname.endsWith(".java")) {
					System.out.println(fname);
					// use ^ ?\*.*\$[^\$]*\$$ to replace cvs tags
				}
			}
		};

		FileUtils.visitRecursively(new File("C:/development/workspace/olat4head/webapp/WEB-INF/src"), fv);

	}

}
