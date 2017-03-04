/*
 *
 * Copyright (C) 2011 Martin Schaef 
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package jayhorn.ws.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;

/**
 * @author schaef
 */
public class Examples {

	final static String PATH_EXAMPLES = "/exs";
	private List<String> examples = new ArrayList<String>();

	public Examples(ServletContext ctx) {

		// list all examples
		String path = ctx.getRealPath("/");
		File examplesDir = new File(path + PATH_EXAMPLES);
		File[] exampleFiles = examplesDir.listFiles();
		
		if (exampleFiles==null) {
			throw new RuntimeException("Dir not found: "+examplesDir);
		}
		
		// load examples
		for (File exampleFile : exampleFiles) {
			if (!exampleFile.isFile() || !exampleFile.getName().endsWith(".java")) {
				continue;
			}			
			String example="/* Error loading example!*/";
			try {
				byte[] encoded = Files.readAllBytes(Paths.get(exampleFile.getAbsolutePath()));
				example = new String(encoded, StandardCharsets.UTF_8);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			example = example.replace("\r", "");
			example = example.replace("\n", "  \\n");
			example = example.replace("\"", "  \\\"");
			examples.add(example);
		}
	}

	public String[] getExamples() {
		return examples.toArray(new String[examples.size()]);
	}

	int currentExample = 0;
	
	/**
	 * Returns a random example
	 * 
	 * @param ctx
	 *            Context
	 * @return Example
	 */
	public String getNextExample() {
		currentExample++;
		
		if (currentExample>=examples.size()) {
			currentExample=0;
		}
		return examples.get(currentExample);
	}
	
}
