package com.zr.security.oauth2.uaa.config;

import com.baomidou.mybatisplus.core.toolkit.ArrayUtils;
import com.zr.security.oauth2.uaa.properties.ClientLoadProperties;
import com.zr.security.oauth2.uaa.properties.ClientProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.builders.InMemoryClientDetailsServiceBuilder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;
import org.springframework.security.oauth2.provider.code.AuthorizationCodeServices;
import org.springframework.security.oauth2.provider.code.InMemoryAuthorizationCodeServices;
import org.springframework.security.oauth2.provider.code.JdbcAuthorizationCodeServices;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenEnhancerChain;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.Arrays;

/**
 * 认证授权服务配置
 */
@Configuration
@EnableAuthorizationServer
public class AuthorizationServer extends AuthorizationServerConfigurerAdapter {

    private String SIGNING_KEY = "zrtokenkey";
    @Resource
    private ClientLoadProperties clientLoadProperties;
    @Autowired
    private ClientDetailsService clientDetailsService;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private RedisConnectionFactory redisConnectionFactory;
    @Autowired
    private AuthorizationCodeServices authorizationCodeServices;
    @Autowired
    private JwtAccessTokenConverter accessTokenConverter;
    @Autowired
    PasswordEncoder passwordEncoder;
    /**
     * 定义token的存储方式
     *
     * @return TokenStore
     */
    @Bean
    public TokenStore tokenStore() {
        //redis令牌存储 （远程调用授权开启）
//        return new RedisTokenStore(redisConnectionFactory);
        //jwt令牌存储
        return new JwtTokenStore(accessTokenConverter());
    }

    @Bean
    public JwtAccessTokenConverter accessTokenConverter() {
        JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
        converter.setSigningKey(SIGNING_KEY); //对称秘钥，资源服务器使用该秘钥来验证
        return converter;
    }

    //将客户端信息存储到数据库
    @Bean
    public ClientDetailsService clientDetailsService(DataSource dataSource) {
        ClientDetailsService clientDetailsService = new JdbcClientDetailsService(dataSource);
        ((JdbcClientDetailsService) clientDetailsService).setPasswordEncoder(passwordEncoder);
        return clientDetailsService;
    }

    /**
     * 用于定义客户端详细信息服务的配置程序。可以初始化客户端详细信息，也可以只引用现有商店。
     *
     * @param clients a configurer that defines the client details service. Client details can be initialized, or you can just refer to an existing store.
     * @throws Exception exception
     */
    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        //yml配置方式
//        InMemoryClientDetailsServiceBuilder builder = clients.inMemory();
//        if (ArrayUtils.isNotEmpty(clientLoadProperties.getClients())) {
//            for (ClientProperties config : clientLoadProperties.getClients()) {
//                builder
//                        //设置客户端和密码
//                        .withClient(config.getClientId()).secret(new BCryptPasswordEncoder().encode(config.getClientSecret()))
//                        //资源列表
//                        .resourceIds("res1")
////                        //设置token有效期
////                        .accessTokenValiditySeconds(7 * 24 * 3600)
////                        //设置refreshToken有效期
////                        .refreshTokenValiditySeconds(7 * 24 * 3600)
//                        //支持的认证方式
//                        .authorizedGrantTypes("refresh_token","client_credentials","implicit" ,"authorization_code", "password")
//                        //false 跳转到授权页面  true 直接发令牌
//                        .autoApprove(false)
//                        //授权域(范围)
//                        .scopes("all")
//                        //回调地址
//                        .redirectUris("http://www.baidu.com");
//            }
//        }

        //数据库读取方式
        clients.withClientDetails(clientDetailsService);
    }

    //令牌管理服务
    @Bean
    public AuthorizationServerTokenServices tokenServices(){
        DefaultTokenServices service = new DefaultTokenServices();
        service.setClientDetailsService(clientDetailsService);//客户端服务消息
        service.setSupportRefreshToken(true);//是否刷新令牌
        service.setTokenStore(tokenStore());//令牌存储策略
        //令牌增强 (远程调用注释此段)
        TokenEnhancerChain tokenEnhancerChain = new TokenEnhancerChain();
        tokenEnhancerChain.setTokenEnhancers(Arrays.asList(accessTokenConverter));
        service.setTokenEnhancer(tokenEnhancerChain);
        //令牌增强 end
        service.setAccessTokenValiditySeconds(7 * 24 * 3600); //设置令牌有效期
        service.setRefreshTokenValiditySeconds(7 * 24 * 3600); //刷新令牌有效期
        return service;
    }

    /**
     * 定义授权和令牌端点以及令牌服务
     *
     * @param endpoints defines the authorization and token endpoints and the token services.
     * @throws Exception exception
     */
    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints
                .authenticationManager(authenticationManager) //认证管理器
                .authorizationCodeServices(authorizationCodeServices) //授权码服务
                .tokenServices(tokenServices()) //令牌管理服务
                .allowedTokenEndpointRequestMethods(HttpMethod.POST); //允许post提交
    }

    //设置授权码模式的授权码如何存取，暂时采用内存方式
//    @Bean
//    public AuthorizationCodeServices authorizationCodeServices(){
//        return new InMemoryAuthorizationCodeServices();
//    }

    //设置授权码模式的授权码如何存取，数据库
    @Bean
    public AuthorizationCodeServices authorizationCodeServices(DataSource dataSource) {
        return new JdbcAuthorizationCodeServices(dataSource);
    }

    /**
     * 定义令牌端点上的安全性约束
     *
     * @throws Exception exception
     */
    @Override
    public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
        security
                .tokenKeyAccess("permitAll()")//oauth/token_key是公开
                .checkTokenAccess("permitAll()") // /oauth/check_token公开
                .allowFormAuthenticationForClients(); //表单认证
    }
}
