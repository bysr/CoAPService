# CoAPService
> ## app包:Android服务器端代码

> ## coaplib: java服务器代码以及java客户端测试代码

> COAP协议：[原文](http://www.jianshu.com/p/535f4fe5821b)

1. CoAP服务器则提供了人们能轻松看懂的URI，如 */thermometers/5* 。在可发现性的使用惯例里，所有资源都可以通过访问 */.well-known/core* 这个地址列出，每个资源可以通过一系列查询参数来筛选，如 */.well-known/core?rt=light_switch* 会列出所有资源类型（*rt, resource type*）为 *light_switch*的资源。

2. 和HTTP协议类似，你可以使用GET, POST, PUT 和 DELETE来操作资源，这种相似性使你可以映射请求到另一个服务器，也就是把CoAP和Web结合。COAP底层基于UDP，协议更加轻盈，请求可以不被确定，也可以确定，根据需求定，由于UDP的不可靠性，CoAP协议采用了双层结构，定义了带有重传的事务处理机制，并且提供资源发现和资源描述等功能。CoAP采用尽可能小的载荷，从而限制了分片。

3. 最有趣的特性要属“*observe*” 设置了。客户端发送GET请求时可以传递一个flag来开启观察者模式(*observation*)。*server*之后会把这个客户端列入特定资源的观察者名单，然后客户端持续监听服务端的响应。它允许我们构建被动接收数据的系统，无论这些数据将在什么时候送达。我们回想到HTTP和*Websocket*的场景，是不是有点像*publisher-subscriber*模式？是不是有点像*Meteor*的*REST for Websocket*。简言之，CoAP既可以单次REST请求，也可以通过*observe*实现实时数据订阅。

> COAP报文格式:[模块说明](http://www.cnblogs.com/littleatp/p/6417567.html)

1. ~.californium-core
californium 核心模块，定义了一系列协议栈核心接口，并提供了Coap协议栈的完整实现，
2. coap：定义了消息类型，消息头，observe机制等，COAP协议中的常量和消息的基本模型MessageObserver 接口实现消息的状态跟踪，重传确认等
3. observe：
COAP协议订阅模块，是协议的一大功能![image](http://o7x0ygc3f.bkt.clouddn.com/Californium%E5%BC%80%E6%BA%90%E6%A1%86%E6%9E%B6%E5%88%86%E6%9E%90/observe%E5%8C%85_01.png)

4. package-network：
network 是协议栈核心机制实现的关键模块，涵盖了网络传输以及协议层的定义以及实现
模块实现了一些关键接口定义，将网络传输端点抽象为Endpoint，



5. ~.element-connector
从core模块剥离的连接器模块，用于抽象网络传输层的接口，使得coap可以同时运行于udp和tcp多种传输协议之上;

6. ~.californium-proxycoap代理模块，用于支持coap2coap、coap2http、http2coap的转换;

7. ~.scandium-core
Coap over DTLS 支持模块，提供了DTLS 传输的Connector实现;

8. ~.demo-xxx 样例程序;

-   其中，californium-core和element-connector是coap技术实现最关键的模块

---

 > 核心接口
 
- 与分层设计对应，框架分为 **transport 传输层**、**protocol 协议层**、**logic 逻辑层**。

1. **transport 传输层 :**  由Connector 提供传输端口的抽象，UDPConnector是其主要实现；数据包通过RawData对象封装；该层还提供CorrelationContext 实现传输层会话数据的读写支持。【**element-connector.jar包**】
 
2. **protocol 协议层 :** 提供了Coap协议栈机制的完整实现；
CoapEndpoint是核心的操作类，数据的编解码通过DataSerializer、DataParser实现，MessageInterceptor提供了消息收发的拦截功能，Request/Response的映射处理由 Matcher实现，Exchange 描述了映射模型；协议栈CoapStack 是一个分层的内核实现，在这里完成分块、重传等机制。

3. **californium-core网络层 :**
logic 逻辑层，定义了CoapClient、CoapServer的入口，包括消息的路由机制，Resource的继承机制；Observe机制的关系维护、状态管理由ObserveManager提供入口。


> 简单说明：MessageInterceptor（消息拦截器）、Matcher（匹配器）、CoapStack（Coap协议栈）

- COAP源码分析：[Californium开源框架之源码分析](http://wudashan.cn/2017/05/21/Californium-Framework-Analysis-01/)