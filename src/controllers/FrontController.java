package controllers;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import annotation.Controller;
import annotation.FieldName;
import annotation.Get;
import annotation.ObjectParam;
import annotation.Post;
import annotation.Param;
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
        try {
            scan();
        } catch (Exception e) {
            getServletContext().setAttribute("initError", e.getMessage());
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");

        String requestUrl = request.getRequestURI().substring(request.getContextPath().length());

        if (requestUrl.contains("?")) {
            requestUrl = requestUrl.substring(0, requestUrl.indexOf("?"));
        }

        try (PrintWriter out = response.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>FrontController</title>");
            out.println("</head>");
            out.println("<body>");

            String initError = (String) getServletContext().getAttribute("initError");
            if (initError != null) {
                out.println("<h2>" + initError + "</h2>");
            } else {
                try {
                    Mapping mapping = urlMappings.get(requestUrl);

                    if (mapping != null) {
                        Class<?> clazz = Class.forName(mapping.getClassName());
                        Object instance = clazz.getDeclaredConstructor().newInstance();
                        Method method = null;
                        for (Method m : clazz.getDeclaredMethods()) {
                            if (m.getName().equals(mapping.getMethodName())) {
                                method = m;
                                break;
                            }
                        }
                        if (method == null) {
                            throw new NoSuchMethodException("Méthode " + mapping.getMethodName() + " introuvable dans la classe " + mapping.getClassName());
                        }

                        Object[] params = getParams(method, request);

                        if (method.getReturnType() == String.class) {
                            out.println("<p>=> " + method.invoke(instance, params) + "</p>");
                        } else if (method.getReturnType() == ModelAndView.class) {
                            ModelAndView modelAndView = (ModelAndView) method.invoke(instance, params);
                            for (Map.Entry<String, Object> entry : modelAndView.getData().entrySet()) {
                                request.setAttribute(entry.getKey(), entry.getValue());
                            }
                            request.getRequestDispatcher(modelAndView.getUrl()).forward(request, response);
                        } else {
                            throw new Exception("Type de retour incorrect");
                        }
                    } else {
                        out.println("<p>Aucune méthode associée à ce chemin URL : " + requestUrl + "</p>");
                    }
                } catch (Exception e) {
                    out.println("<h2>" + e.getMessage() + "</h2>");
                    e.printStackTrace(out);
                }
            }
            out.println("</body>");
            out.println("</html>");
        }
    }

    private void scan() throws Exception {
        ServletContext context = getServletContext();
        String packageName = context.getInitParameter("controller_package");

        if (packageName == null || packageName.isEmpty()) {
            throw new Exception("Package absent ou vide");
        }

        packageName = packageName.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(packageName);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if (resource.getProtocol().equals("file")) {
                File file = new File(resource.toURI());
                scanControllers(file, packageName);
            }
        }
    }

    private void scanControllers(File directory, String packageName) throws Exception {
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
                                if (urlMappings.containsKey(url)) {
                                    throw new Exception("URL déjà associé à une méthode");
                                } else {
                                    urlMappings.put(url, new Mapping(className, method.getName()));
                                }
                            } else if (method.isAnnotationPresent(Post.class)) {
                                Post post = method.getAnnotation(Post.class);
                                String url = post.value();
                                if (urlMappings.containsKey(url)) {
                                    throw new Exception("URL déjà associé à une méthode");
                                } else {
                                    urlMappings.put(url, new Mapping(className, method.getName()));
                                }
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    throw new Exception("Classe non trouvée : " + className, e);
                }
            }
        }
    }

    private Object[] getParams(Method method, HttpServletRequest request) throws Exception {
        Parameter[] params = method.getParameters();
        Object[] paramsValue = new Object[params.length];

        for (int i = 0; i < paramsValue.length; i++) {
            Param requestParam = params[i].getAnnotation(Param.class);
            ObjectParam objectParam = params[i].getAnnotation(ObjectParam.class);
            if (requestParam != null) {
                String paramName = requestParam.value();
                String paramValue = request.getParameter(paramName);
                paramsValue[i] = paramValue;
            } else if (objectParam != null) {
                Class<?> clazz = params[i].getType();
                Object obj = clazz.getDeclaredConstructor().newInstance();
                Map<String, String[]> parameterMap = request.getParameterMap();
                for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                    String paramName = entry.getKey();
                    if (paramName.startsWith(objectParam.value() + ".")) {
                        String fieldName = paramName.substring(objectParam.value().length() + 1);
                        Field field = findFieldByName(clazz, fieldName);
                        if (field != null) {
                            field.setAccessible(true);
                            String[] values = entry.getValue();
                            if (field.getType() == String.class) {
                                field.set(obj, values[0]);
                            } else if (field.getType() == Integer.class) {
                                field.set(obj, Integer.parseInt(values[0]));
                            }
                        }
                    }
                }
                paramsValue[i] = obj;
            }
        }
        return paramsValue;
    }

    private Field findFieldByName(Class<?> clazz, String name) {
        for (Field field : clazz.getDeclaredFields()) {
            FieldName fieldNameAnnotation = field.getAnnotation(FieldName.class);
            if (fieldNameAnnotation != null && fieldNameAnnotation.value().equals(name)) {
                return field;
            } else if (field.getName().equals(name)) {
                return field;
            }
        }
        return null;
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
