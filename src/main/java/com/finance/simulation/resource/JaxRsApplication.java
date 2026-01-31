package com.finance.simulation.resource;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS アプリケーション設定
 *
 * すべてのREST APIは /api パス以下で提供されます。
 */
@ApplicationPath("/api")
public class JaxRsApplication extends Application {
    // 自動的にリソースクラスをスキャン
}
