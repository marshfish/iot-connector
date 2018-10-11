#设备TCP中转服务器
* 通过暴露http接口接受服务器请求并通过tcp转发给设备，来自设备的请求也通过tcp进行响应
* 由于springMVC仅能转发http请求，为了方便，tcp请求交给自定义的mvc容器控制，由自定义的mvc容器转发tcp数据包，
   业务逻辑在@InstructionManager注解的controller中实现即可，注解@Instruction用于标识上行协议号，
* 默认数据格式（方法参数&返回值）均为unicode的字符串，若无需响应tcp请求给设备，方法返回值设为void即可
* 由于SpringBoot打成jar包，难以通过jarEntry或classloader实现根据注解自动扫描，需手动再yml里配置controller位置
* 默认http请求端口8766， tcp端口8765

#手环相关   
* 手环协议号约定详见《智能手表公版协议.docx》
* 手环配置的服务器地址需要通过发送短信配置，格式 #ip#=xxx.xxx.xxx.xx,xxx#，默认是厂家的代理服务器