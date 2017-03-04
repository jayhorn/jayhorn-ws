/*
 * 
 * Copyright (C) 2011 Martin Schaef and Stephan Arlt
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Scanner;
import java.util.UUID;

import javax.servlet.ServletContext;

import jayhorn.solver.ProverFactory;
import jayhorn.solver.spacer.SpacerProverFactory;
import jayhorn.utils.Stats;

/**
 * @author schaef
 */
public class Runner {


	final static String PATH_POSTS = "/tmp";
	final static String SUB_FOLDER = "/webhorn";
	final static String PATH_LIBS = "/WEB-INF/lib";

	/**
	 * Max length of a program to be checked
	 */
	final static int MAX_LENGTH = 1024;

	public static String ErrorMessage;


	public static String run(ServletContext ctx, String code)
			throws Exception {
		// pre-analyze code
		if (code.length() > MAX_LENGTH) {
			throw new RuntimeException(
					"Sorry, this program is too large for this web service.");
		}
		if (code.contains("package")) {
			throw new RuntimeException(
					"Sorry, the keyword 'package' is not allowed in this web service.");
		}

		// create UUID
		String uuid = String.format("jayhorn%s", UUID.randomUUID().toString()
				.replace("-", ""));
		String fileName = String.format("%s.java", uuid);

		// create a subfolder to make sure that there is only on file in that
		// folder. Just to be safe, delete the entire directory.
		String sourceDirName = String.format("%s/%s", PATH_POSTS, String.format("src_%s", UUID.randomUUID().toString()
				.replace("-", "")));
		File sourceDir = new File(sourceDirName);
		if (sourceDir!=null && sourceDir.exists()) {
			delete(sourceDir);
		}
		
		if (!sourceDir.mkdir()) {
			throw new RuntimeException("Could not create folder.");
		}
		
		String pathName = String.format("%s/%s", sourceDir.getAbsolutePath(), fileName);
		String clazz = String.format("public class %s {}", uuid);

		// create source file
		File sourceFile = new File(pathName);
		try (FileOutputStream fileStream = new FileOutputStream(sourceFile);
			OutputStreamWriter writer = new OutputStreamWriter(fileStream, "UTF-8");) {
			writer.write(String.format("%s %s", code, clazz));
			writer.flush();
			writer.close();
		} catch (Throwable t) {
			return null;
		}

		File classDir = new File(sourceDir.getAbsolutePath()+"/bin");
		if (!classDir.mkdir()) {
			throw new RuntimeException("Could not create folder "+classDir);
		}

		// compile the source file.
		String javac_command = String.format("javac -g %s -d %s", sourceFile.getAbsolutePath(), classDir.getAbsolutePath());
		
		System.out.println("Running "+ javac_command);
		
		ProcessBuilder pb = new ProcessBuilder(javac_command.split(" "));
//		pb.redirectOutput(Redirect.INHERIT);
//		pb.redirectError(Redirect.PIPE);

		Process p = pb.start();

		try (Scanner scanner = new Scanner(p.getErrorStream(), "UTF-8")) {
			p.waitFor();
			 
			if (p.exitValue()!=0) {
				String err = scanner.useDelimiter("\\A").next();
				System.out.println(err);
				throw new ParserException(parseError(err));
			}
		} catch (InterruptedException e) {			
			return null;
		} finally {
			System.setErr(new PrintStream(new FileOutputStream(
					FileDescriptor.err)));
		}
		
		System.out.println("Running on " + classDir.getAbsolutePath());
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ByteArrayOutputStream baes = new ByteArrayOutputStream();
		System.setOut(new PrintStream(baos));
		System.setErr(new PrintStream(baes));

		String libPath = ctx.getRealPath("/");
		libPath = new File(libPath, PATH_LIBS).getPath();
		

		String output = null;
		try {			
			jayhorn.Main.main(new String[]{"-j", classDir.getAbsolutePath()});
			output = Stats.stats().toString();
		} catch (Throwable e) {
			e.printStackTrace();
			output = e.toString();
		} finally {
			System.setOut(new PrintStream(new FileOutputStream(
					FileDescriptor.out)));
			System.setErr(new PrintStream(new FileOutputStream(
					FileDescriptor.err)));
			// delete source file
			try {
				if (!sourceFile.delete()) {
					System.err.println("Warning, source file not deleted!");
				}
				if (sourceDir!=null && sourceDir.exists()) {
					delete(sourceDir);
				}				

			} catch (Throwable t) {
				
			}
		}
		System.out.println(baos.toString("UTF-8"));
		System.err.println(baes.toString("UTF-8"));
		
		if (output == null
				|| baes.toString("UTF-8").contains("soot.CompilationDeathException")) {
			throw new ParserException(parseError(baos.toString("UTF-8")));
		} else {
			ErrorMessage = null;
		}

		return output;
	}

	private static HashMap<Integer, String> parseError(String error) {
		ErrorMessage = null;
		String[] lines = error.split("\n");
		HashMap<Integer, String> errorMessages = new HashMap<Integer, String>();
		if (lines == null)
			return errorMessages;
		boolean found = false;
		Integer lastLine = 0;

		for (String line : lines) {

			if (line.contains(".java")) {
				String substr = line.substring(line.indexOf(":") + 1);
				int firstComma = substr.indexOf(",");
				int firstCol = substr.indexOf(":");
				int idx = firstComma;
				if (firstComma == -1 || (firstCol > -1 && firstCol < idx)) {
					idx = firstCol;
				}
				if (idx < 0) {
					continue;
				}
				lastLine = Integer.parseInt(substr.substring(0, idx));
				if (substr.contains("error:")) {
					String msg = substr.substring(substr.indexOf("error:"), substr.length());
					if (errorMessages.containsKey(lastLine)) {
						msg = errorMessages.get(lastLine) + "\\n" +msg;
					}					
					errorMessages.put(lastLine, msg);
					
				}
				found = true;
			} else {
				if (found) {
					String msg = "";
					if (errorMessages.containsKey(lastLine)) {
						msg = errorMessages.get(lastLine) + "\\n";
					}
					msg += line.substring(line.indexOf(":") + 1);
					errorMessages.put(lastLine, msg);
				}
				found = false;
			}

		}
		return errorMessages;
	}

	protected static void delete(File f) throws IOException {
		if (f.isDirectory()) {
			File[] files = f.listFiles();
			if (files!=null) {
				for (File c : files) {
					delete(c);
				}
			}
		}
		if (!f.delete()) {
			throw new IOException("Failed to delete file: " + f);
		}
	}

}
