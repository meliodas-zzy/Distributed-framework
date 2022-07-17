package zzy.distributed.spring;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.w3c.dom.Element;
import zzy.distributed.consumer.ConsumerFactoryBean;

public class ConsumerFactoryBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {
    protected Class getBeanClass(Element element) {
        return ConsumerFactoryBean.class;
    }

    protected void doParse(Element element, BeanDefinitionBuilder bean) {

        try {
            String timeOut = element.getAttribute("timeout");
            String targetInterface = element.getAttribute("interface");
            String clusterStrategy = element.getAttribute("clusterStrategy");
            String remoteAppKey = element.getAttribute("remoteAppKey");
            String groupName = element.getAttribute("groupName");

            bean.addPropertyValue("timeout", Integer.parseInt(timeOut));
            bean.addPropertyValue("targetInterface", Class.forName(targetInterface));
            bean.addPropertyValue("remoteAppKey", remoteAppKey);

            if (StringUtils.isNotBlank(clusterStrategy)) {
                bean.addPropertyValue("clusterStrategy", clusterStrategy);
            }
            if (StringUtils.isNotBlank(groupName)) {
                bean.addPropertyValue("groupName", groupName);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
