package name.yumaa;

import name.yumaa.ChromeLogger4J;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * This example servlet class shows how to use ChromeLogger4J
 *
 * @author Didenko Victor
 */
public class ExampleServlet extends HttpServlet {

	@Override
	public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		// initialize chrome logger
		ChromeLogger4J console = new ChromeLogger4J(response);

		/*
		ArrayList<String> strArr = new ArrayList<String>() {{
			add("line1");
			add("line2");
			add("line3");
		}};

		console.stack = true;
		console.log(strArr);
		*/

		final HashMap<String,String> strMap = new HashMap<String,String>() {{
			put("name", "Vasya");
			put("surname", "Pupkin");
			put("email", "vasya.pupkin@mailinator.com");
		}};

		console.log(strMap);

		HashMap<String,Object> map = new HashMap<String,Object>() {{
			put("company", "Company Inc");
			put("director", strMap);
			put("since", new Date());
		}};

		console.log(map);

		/*
		console.reflect = true;
		console.reflectmethods = true;
		console.warn("console", console);
		*/

	}

}
