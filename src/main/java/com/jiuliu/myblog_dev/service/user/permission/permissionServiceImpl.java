/*
 * [permissionServiceImpl.java]
 * --------------------------------------------------------------------------------
 * This software is licensed under the MIT License.
 * However, any distribution or modification must retain this copyright notice.
 * See LICENSE for full terms.
 * --------------------------------------------------------------------------------
 * author: "Jiu Liu"
 * author_contact: "QQ: 3209174373, GitHub: https://github.com/DCSCDF"
 * license: "MIT"
 * license_exception: "Mandatory attribution retention"
 * UpdateTime: 2026/1/24 23:24
 */

package com.jiuliu.myblog_dev.service.user.permission;

import cn.dev33.satoken.util.SaResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class permissionServiceImpl implements permissionService {

    private static final Logger log = LoggerFactory.getLogger(permissionServiceImpl.class);


    @Override
    public SaResult getPermission() {
        return null;
    }
}
