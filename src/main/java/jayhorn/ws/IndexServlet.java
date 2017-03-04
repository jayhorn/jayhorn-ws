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

package jayhorn.ws;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jayhorn.ws.util.ParserException;
import jayhorn.ws.util.Examples;
import jayhorn.ws.util.Runner;

/**
 * @author schaef
 */
@SuppressWarnings("serial")
public class IndexServlet extends HttpServlet {

	Examples examples = null;
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		try {
			// load all examples
			if (this.examples==null) {
				examples = new Examples(req.getServletContext());
			}
			req.setAttribute("examples", examples.getExamples());
			String example_idx = req.getParameter("examplecounter");
			if (example_idx==null || example_idx.isEmpty()) {
				example_idx = "0";
			}
			req.setAttribute("exampleIdx", example_idx);
			forward(req, resp);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {		
		try {
			if (this.examples==null) {
				examples = new Examples(req.getServletContext());
			}
			req.setAttribute("examples",
					examples.getExamples());
			String code = req.getParameter("code");

			String example_idx = req.getParameter("examplecounter");
			if (example_idx==null || example_idx.isEmpty()) {
				example_idx = "0";
			}
			req.setAttribute("exampleIdx", example_idx);
			
			String output = Runner.run(
					req.getServletContext(), code);
			
			if (output==null) {
				System.out.println("No output generated");
				return;
			}
			System.out.println(output);
			req.setAttribute("output", output);

		} catch (ParserException e) {
//			for (Entry<Integer, String> entry : e.getErrorMessages().entrySet()) {
//				System.out.println(entry.getKey() + " " + entry.getValue());
//			}
//			e.printStackTrace();
			req.setAttribute("parsererror", e.getErrorMessages());
		} catch (RuntimeException e) {
			e.printStackTrace();
			req.setAttribute("error", e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			forward(req, resp);
		}
	}

	protected void forward(HttpServletRequest req, HttpServletResponse resp) {
		try {
			// forward request
			RequestDispatcher rqd = req.getRequestDispatcher("index.jsp");
			if (rqd!=null) {
				rqd.forward(req, resp);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
