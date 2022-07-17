package zzy.distributed.test;

public class MainClient {
    public static void main(String[] args) {
        final HelloService helloService = new HelloServiceImpl();
        long count = 100000000l;
        for (int i = 0; i < count; i++) {
            try {
                String result = helloService.sayHello("zhengzhanyu,i=" + i);
                System.out.println(result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.exit(0);
    }
}
