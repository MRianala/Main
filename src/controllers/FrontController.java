package controllers;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import annot.Controller;
import annot.Get;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utilities.Mapping;
import utilities.ModelAndView;

public class FrontController extends HttpServlet {

    private HashMap<String, Mapping> urlMappings = new HashMap<>();

    @Override
    public void init() throws ServletException {
        scan();
    }
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");

        String requestUrl = request.getRequestURI().substring(request.getContextPath().length());

        try (PrintWriter out = response.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>FrontController</title>");
            out.println("</head>");
            out.println("<body>");

            Mapping mapping = urlMappings.get(requestUrl);

            if (mapping != null) {
                try {
                    Class<?> clazz = Class.forName(mapping.getClassName());
                    Object instance = clazz.getDeclaredConstructor().newInstance();
                    Method method = instance.getClass().getMethod(mapping.getMethodName());
                    // Raha String ny retour
                    if (method.getReturnType() == String.class) {
                        out.println("<p>=> " + method.invoke(instance) + "</p>");
                    }
                    // Raha Model and View
                    else if(method.getReturnType() == ModelAndView.class) {
                        ModelAndView modelAndView = (ModelAndView) method.invoke(instance);
                        for (Map.Entry<String, Object> entry : modelAndView.getData().entrySet()) {
                            request.setAttribute(entry.getKey(), entry.getValue());
                        }
                        request.getRequestDispatcher(modelAndView.getUrl()).forward(request, response);
                    }else{
                        throw new Exception("Type de retour incorrect");
                    }
                } catch (Exception e) {e.printStackTrace();}
            } else {
                out.println("<p>Aucune méthode associée à ce chemin URL : " + requestUrl + "</p>");
            }

            out.println("</body>");
            out.println("</html>");
        }
    }

    private void scan() {
        try {
            ServletContext context = getServletContext();
            String packageName = context.getInitParameter("controller_package");

            if (packageName == null || packageName.isEmpty()) {
                throw new Exception("Package abscent ou vide");
            }

            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(packageName.replace('.', '/'));

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if (resource.getProtocol().equals("file")) {
                    File file = new File(resource.toURI());
                    scanControllers(file, packageName);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scanControllers(File directory, String packageName) {
        if (!directory.exists()) {
            return;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                scanControllers(file, packageName + "." + file.getName());
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                try {
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(Controller.class)) {
                        Method[] methods = clazz.getDeclaredMethods();
                        for (Method method : methods) {
                            if (method.isAnnotationPresent(Get.class)) {
                                Get get = method.getAnnotation(Get.class);
                                String url = get.value();
                                if (urlMappings.containsKey(className)) {
                                    throw new Exception("url deja associer à une methode");
                                }else{
                                    urlMappings.put(url, new Mapping(className, method.getName()));
                                }
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

}
