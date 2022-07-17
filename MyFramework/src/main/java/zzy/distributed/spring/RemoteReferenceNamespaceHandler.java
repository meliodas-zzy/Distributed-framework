package zzy.distributed.spring;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class RemoteReferenceNamespaceHandler extends NamespaceHandlerSupport {
    @Override
    public void init() {
        registerBeanDefinitionParser("reference", new ConsumerFactoryBeanDefinitionParser());
    }
}
