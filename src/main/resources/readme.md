#设备TCP中转服务器
* 通过RabbitMq接受服务器请求并通过tcp/udp转发给设备，来自设备的请求也通过tcp/udp进行响应
* 由于springMVC仅能转发http请求，为了方便，自定义了一套mvc框架，仅支持rest请求（json + Http post）
  默认数据格式（方法参数&返回值）均为unicode的字符串，若无需响应tcp请求给设备，方法返回值设为void即可
* 借鉴vert.x的事件驱动和netty的pipeline
  暂时使用rabbitmq做rpc，以后可替换成vert.x自带的EventBus或grpc等，但不管替换什么rpc框架，都不与业务耦合
  dispatcher端与connector端始终可以通过事件进行通信，事件相当于dispatcher与connector的自定义协议
* 如果要添加新的事件，需要：
  1.约定dispatcher端和connector的事件协议，如事件类型、流水号、是否需要双工通信等等
  2.添加事件配置，详见枚举com.hc.equipment.type.EventTypeEnum
  3.在dispatcher端和connector端添加EventHandler，如果仅需单工通信，仅在一端添加即可。EventHandler分两种，异步的需继承
  AsyncEventHandler，同步则需继承SyncEventHandler。异步事件处理器重写handler与setReceivedEventType方法即可实现异步，
  同步事件处理器仅需重写setReceivedEventType方法设置事件类型（枚举中定义）
  4.将EventHandler添加到pipeline或defaultPipeline，pipeline的设计较为灵活，借鉴了netty的pipeline责任链
  pipe是由多个事件处理器组成的流水线，每一个新请求对应一个pipeline，可在运行时动态添加/卸载事件处理器，自定义添加事件处理器及事件
  5.如果需要全双工通信（如connector发送请求，dispatcher响应，connector获取响应），需要推送给dispatcher端，MqConnector提供了同步推送和
  异步推送的方法，异步顾名思义，同步则阻塞当前线程等待直到响应返回，其最大阻塞时间可在yml中配置
* 目前基于性能考虑，回调交给connector处理，相比与交给dispatcher处理减少一次网络IO成本，但这实际上是dispather的职责
  以后如果把rabbitmq换成高性能rpc框架如grpc，可将对回调的处理交给dispathcher端
#手环相关   
* 手环协议号约定详见《智能手表公版协议.docx》
* 手环配置的服务器地址需要通过发送短信配置，格式 #ip#=xxx.xxx.xxx.xx,xxx#，默认是厂家的代理服务器
    ##协议格式
    * 包头： IW
    * 协议号：上行(设备服务器) AP[两位数字]，下行(服务器设备) BP[两位数字]，数字一样表示数据包及数据包响应
    * 参数：数据包内容
    * 结束符：#
    * 所有与中文有关，例如地址，都使用UNICODE编码
    * 数据包中所有标点符号(除了下发地址中的标点)均为英文半角。