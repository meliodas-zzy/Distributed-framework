package zzy.distributed.test;

public class HelloServiceImpl implements HelloService{
    @Override
    public String sayHello(String str) {
        return "hello" + str + "!";
    }
}
