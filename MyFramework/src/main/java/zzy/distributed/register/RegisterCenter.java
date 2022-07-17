package zzy.distributed.register;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.serialize.SerializableSerializer;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import zzy.distributed.service.ConsumerService;
import zzy.distributed.service.ProviderService;
import zzy.distributed.utils.IpUtil;
import zzy.distributed.utils.PropertyUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RegisterCenter implements RegisterCenter4Provider, RegisterCenter4Consumer{

    private static RegisterCenter registerCenter = new RegisterCenter();

    //key:服务提供者接口名，value：服务提供者服务方法列表
    private static final Map<String, List<ProviderService>> providerServiceMap = new ConcurrentHashMap<>();

    private static final Map<String, List<ProviderService>> serviceMetaDataMap = new ConcurrentHashMap<>();

    //zookeeper服务地址
    private static String ZK_SERVICE = PropertyUtil.getZkService();
    //zookeeper会话超时时间
    private static int ZK_SESSION_TIME_OUT = PropertyUtil.getZkSessionTimeout();
    //zookeeper链接超时时间
    private static int ZK_CONNECTION_TIME_OUT = PropertyUtil.getZkConnectionTimeout();
    private static String ROOT_PATH = "/config_register";
    public static String PROVIDER_TYPE = "provider";
    public static String CONSUMER_TYPE = "consumer";
    private static volatile ZkClient zkClient = null;

    private RegisterCenter() {
    }

    public static RegisterCenter getSingletonInstance() {
        return registerCenter;
    }

    @Override
    public void initProviderMap(String remoteAppKey, String groupName) {
        if (MapUtils.isEmpty(serviceMetaDataMap)) {
            serviceMetaDataMap.putAll(fetchOrUpdateServiceMetaData(remoteAppKey, groupName));
        }
    }

    @Override
    public Map<String, List<ProviderService>> getServiceMetaData4Consumer() {
        return serviceMetaDataMap;
    }

    @Override
    public void registerConsumer(ConsumerService consumerService) {
        if (consumerService == null) {
            return;
        }

        //连接zk,注册服务
        synchronized (RegisterCenter.class) {

            if (zkClient == null) {
                zkClient = new ZkClient(ZK_SERVICE, ZK_SESSION_TIME_OUT, ZK_CONNECTION_TIME_OUT, new SerializableSerializer());
            }
            //创建 ZK命名空间/当前部署应用APP命名空间/
            boolean exist = zkClient.exists(ROOT_PATH);
            if (!exist) {
                zkClient.createPersistent(ROOT_PATH, true);
            }


            //创建服务消费者节点
            String remoteAppKey = consumerService.getRemoteAppKey();
            String groupName = consumerService.getGroupName();
            String serviceNode = consumerService.getServiceItf().getName();
            String servicePath = ROOT_PATH + "/" + remoteAppKey + "/" + groupName + "/" + serviceNode + "/" + CONSUMER_TYPE;
            exist = zkClient.exists(servicePath);
            if (!exist) {
                zkClient.createPersistent(servicePath, true);
            }

            //创建当前服务器节点
            String localIp = IpUtil.localIp();
            String currentServiceIpNode = servicePath + "/" + localIp;
            exist = zkClient.exists(currentServiceIpNode);
            if (!exist) {
                //注意,这里创建的是临时节点
                zkClient.createEphemeral(currentServiceIpNode);
            }
        }
    }

    @Override
    public void registerProvider(final List<ProviderService> serviceMetaData) {
        if (CollectionUtils.isEmpty(serviceMetaData)) {
           return;
        }

        //连接zookeeper，注册服务
        synchronized (RegisterCenter.class) {
            for (ProviderService provider : serviceMetaData) {
                String serviceItfKey = provider.getServiceItf().getName();
                List<ProviderService> providers = providerServiceMap.get(serviceItfKey);
                if (Objects.isNull(providers)) {
                    providers = new ArrayList<>();
                }
                providers.add(provider);
                providerServiceMap.put(serviceItfKey, providers);
            }

            if (Objects.isNull(zkClient)) {
                zkClient = new ZkClient(ZK_SERVICE, ZK_SESSION_TIME_OUT,ZK_CONNECTION_TIME_OUT,
                        new SerializableSerializer());
            }

            //创建ZK命名空间，当前部署应用的命名空间
            String APP_KEY = serviceMetaData.get(0).getAppKey();
            String ZK_PATH = ROOT_PATH + "/" + APP_KEY;
            boolean exist = zkClient.exists(ZK_PATH);
            if (!exist) {
                zkClient.createPersistent(ZK_PATH, true);
            }
            for (Map.Entry<String, List<ProviderService>> entry : providerServiceMap.entrySet()) {
                //服务分组
                String groupName = entry.getValue().get(0).getGroupName();
                //服务提供者
                String serviceNode = entry.getKey();
                String servicePath = ZK_PATH + "/" + groupName + "/" + serviceNode + "/" + PROVIDER_TYPE;
                exist = zkClient.exists(servicePath);
                if (!exist) {
                    zkClient.createPersistent(servicePath,true);
                }

                //创建服务器节点
                int serverPort = entry.getValue().get(0).getServerPort();
                int weight = entry.getValue().get(0).getWeight();
                int workerThreads = entry.getValue().get(0).getWorkerThreads();
                String localIp = IpUtil.localIp();
                String currentServiceIpNode = servicePath + "/" +localIp + "|" + weight + "|"
                        + workerThreads + "|" + groupName;
                exist = zkClient.exists(currentServiceIpNode);
                if (!exist) {
                    zkClient.createEphemeral(currentServiceIpNode);
                }

                //监听注册服务的变化，同时更新数据到本地内存
                zkClient.subscribeChildChanges(servicePath, new IZkChildListener() {
                    @Override
                    public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
                        if (Objects.isNull(currentChilds)) {
                            currentChilds = new ArrayList<>();
                        }

                        //存活的服务IP列表
                        List<String> activeServiceIpList = Lists.newArrayList(Lists.transform(currentChilds,
                                new Function<String, String>() {
                                    @Override
                                    public String apply(String input) {
                                        return StringUtils.split(input, "|")[0];
                                    }
                                }));
                        refreshActivityService(activeServiceIpList);
                    }
                });
            }
        }
    }

    @Override
    public Map<String, List<ProviderService>> getProviderServiceMap() {
        return providerServiceMap;
    }

    //自动刷新当前存活服务提供者的列表数据
    private void refreshActivityService(List<String> serviceIpList) {
        if (Objects.isNull(serviceIpList)) {
            serviceIpList = new ArrayList<>();
        }

        Map<String, List<ProviderService>> currentServiceMetaDataMap = new ConcurrentHashMap<>();
        for (Map.Entry<String, List<ProviderService>> entry : providerServiceMap.entrySet()) {
            String key = entry.getKey();
            List<ProviderService> providerServices = entry.getValue();

            List<ProviderService> serviceMetaDataModelList = currentServiceMetaDataMap.get(key);
            if (serviceMetaDataModelList == null) {
                serviceMetaDataModelList = Lists.newArrayList();
            }

            for (ProviderService serviceMetaData : providerServices) {
                if (serviceIpList.contains(serviceMetaData.getServerIp())) {
                    serviceMetaDataModelList.add(serviceMetaData);
                }
            }
            currentServiceMetaDataMap.put(key, serviceMetaDataModelList);
        }
        providerServiceMap.clear();
        //System.out.println("currentServiceMetaDataMap,"+ JSON.toJSONString(currentServiceMetaDataMap));
        providerServiceMap.putAll(currentServiceMetaDataMap);
    }

    private Map<String, List<ProviderService>> fetchOrUpdateServiceMetaData(String remoteAppKey, String groupName) {
        final Map<String, List<ProviderService>> providerServiceMap = Maps.newConcurrentMap();
        //连接zk
        synchronized (RegisterCenter.class) {
            if (zkClient == null) {
                zkClient = new ZkClient(ZK_SERVICE, ZK_SESSION_TIME_OUT, ZK_CONNECTION_TIME_OUT, new SerializableSerializer());
            }
        }

        //从ZK获取服务提供者列表
        String providePath = ROOT_PATH + "/" + remoteAppKey + "/" + groupName;
        List<String> providerServices = zkClient.getChildren(providePath);

        for (String serviceName : providerServices) {
            String servicePath = providePath + "/" + serviceName + "/" + PROVIDER_TYPE;
            List<String> ipPathList = zkClient.getChildren(servicePath);
            for (String ipPath : ipPathList) {
                String serverIp = StringUtils.split(ipPath, "|")[0];
                String serverPort = StringUtils.split(ipPath, "|")[1];
                int weight = Integer.parseInt(StringUtils.split(ipPath, "|")[2]);
                int workerThreads = Integer.parseInt(StringUtils.split(ipPath, "|")[3]);
                String group = StringUtils.split(ipPath, "|")[4];

                List<ProviderService> providerServiceList = providerServiceMap.get(serviceName);
                if (providerServiceList == null) {
                    providerServiceList = Lists.newArrayList();
                }
                ProviderService providerService = new ProviderService();

                try {
                    providerService.setServiceItf(ClassUtils.getClass(serviceName));
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }

                providerService.setServerIp(serverIp);
                providerService.setServerPort(Integer.parseInt(serverPort));
                providerService.setWeight(weight);
                providerService.setWorkerThreads(workerThreads);
                providerService.setGroupName(group);
                providerServiceList.add(providerService);

                providerServiceMap.put(serviceName, providerServiceList);
            }

            //监听注册服务的变化,同时更新数据到本地缓存
            zkClient.subscribeChildChanges(servicePath, new IZkChildListener() {
                @Override
                public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
                    if (currentChilds == null) {
                        currentChilds = Lists.newArrayList();
                    }
                    currentChilds = Lists.newArrayList(Lists.transform(currentChilds, new Function<String, String>() {
                        @Override
                        public String apply(String input) {
                            return StringUtils.split(input, "|")[0];
                        }
                    }));
                    refreshServiceMetaDataMap(currentChilds);
                }
            });
        }
        return providerServiceMap;
    }

    private void refreshServiceMetaDataMap(List<String> serviceIpList) {
        if (serviceIpList == null) {
            serviceIpList = Lists.newArrayList();
        }

        Map<String, List<ProviderService>> currentServiceMetaDataMap = Maps.newHashMap();
        for (Map.Entry<String, List<ProviderService>> entry : serviceMetaDataMap.entrySet()) {
            String serviceItfKey = entry.getKey();
            List<ProviderService> serviceList = entry.getValue();

            List<ProviderService> providerServiceList = currentServiceMetaDataMap.get(serviceItfKey);
            if (providerServiceList == null) {
                providerServiceList = Lists.newArrayList();
            }
            //如果contains返回false说明服务下线了，就不需要添加
            for (ProviderService serviceMetaData : serviceList) {
                if (serviceIpList.contains(serviceMetaData.getServerIp())) {
                    providerServiceList.add(serviceMetaData);
                }
            }
            currentServiceMetaDataMap.put(serviceItfKey, providerServiceList);
        }

        serviceMetaDataMap.clear();
        serviceMetaDataMap.putAll(currentServiceMetaDataMap);
    }
}
