package utilities;

public class Mapping {
    String className ; 
    String  methodName ;

    public Mapping() {}
    public Mapping(String className, String methodName) {
        this.className = className;
        this.methodName = methodName;
    }

    public String getClassName() {
        return className;
    }
    public String getMethodName() {
        return methodName;
    }
}
