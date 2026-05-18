import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/login'
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/auth/Login.vue')
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/auth/Register.vue')
    },
    // 管理端路由
    {
      path: '/admin',
      component: () => import('@/views/admin/AdminLayout.vue'),
      meta: { title: '管理后台', requiresAuth: true, userType: 3 },
      children: [
        {
          path: '',
          name: 'adminDashboard',
          component: () => import('@/views/admin/Dashboard.vue'),
          meta: { title: '首页' }
        },
        // 用户管理
        {
          path: 'user/info/admin',
          name: 'adminUserAdmin',
          component: () => import('@/views/admin/user/AdminList.vue'),
          meta: { title: '管理员信息' }
        },
        {
          path: 'user/info/landlord',
          name: 'adminUserLandlord',
          component: () => import('@/views/admin/user/LandlordList.vue'),
          meta: { title: '房东信息' }
        },
        {
          path: 'user/info/tenant',
          name: 'adminUserTenant',
          component: () => import('@/views/admin/user/TenantList.vue'),
          meta: { title: '租客信息' }
        },
        {
          path: 'user/realname',
          name: 'adminRealname',
          component: () => import('@/views/admin/user/RealnameAudit.vue'),
          meta: { title: '实名认证审核' }
        },
        // 房源管理
        {
          path: 'house/audit',
          name: 'adminHouseAudit',
          component: () => import('@/views/admin/house/HouseAudit.vue'),
          meta: { title: '房源审核管理' }
        },
        {
          path: 'house/monitor',
          name: 'adminHouseMonitor',
          component: () => import('@/views/admin/house/HouseMonitor.vue'),
          meta: { title: '房源监管' }
        },
        {
          path: 'house/analysis',
          name: 'adminHouseAnalysis',
          component: () => import('@/views/admin/house/HouseAnalysis.vue'),
          meta: { title: '房源数据分析' }
        },
        // 合同管理
        {
          path: 'contract/monitor',
          name: 'adminContractMonitor',
          component: () => import('@/views/admin/rental/ContractMonitor.vue'),
          meta: { title: '合同监管' }
        },
        {
          path: 'contract/alert',
          name: 'adminContractAlert',
          component: () => import('@/views/admin/rental/ContractAlert.vue'),
          meta: { title: '合同到期提醒' }
        },
        // 帖子举报管理
        {
          path: 'rating',
          name: 'adminRating',
          component: () => import('@/views/admin/rating/RatingManage.vue'),
          meta: { title: '帖子举报管理' }
        },
        // 租后服务监管
        {
          path: 'after',
          name: 'adminAfter',
          component: () => import('@/views/admin/after/AfterMonitor.vue'),
          meta: { title: '租后服务监管' }
        },
        // 财务管理
        {
          path: 'finance/order',
          name: 'adminOrder',
          component: () => import('@/views/admin/finance/OrderManage.vue'),
          meta: { title: '订单管理' }
        },
        {
          path: 'finance/income',
          name: 'adminIncome',
          component: () => import('@/views/admin/finance/IncomeAnalysis.vue'),
          meta: { title: '收入分析' }
        },
        // 权限管理
        {
          path: 'permission/role',
          name: 'adminRole',
          component: () => import('@/views/admin/permission/RoleManage.vue'),
          meta: { title: '角色管理' }
        },
        {
          path: 'permission/admin',
          name: 'adminManage',
          component: () => import('@/views/admin/permission/AdminManage.vue'),
          meta: { title: '管理员管理' }
        },
        // 数据统计
        {
          path: 'statistics/business',
          name: 'adminBusiness',
          component: () => import('@/views/admin/statistics/BusinessStats.vue'),
          meta: { title: '业务数据统计' }
        },
        {
          path: 'statistics/report',
          name: 'adminReport',
          component: () => import('@/views/admin/statistics/ReportAnalysis.vue'),
          meta: { title: '支出分析报表' }
        },
        // 客服工作台
        {
          path: 'customer-service',
          name: 'adminCustomerService',
          component: () => import('@/views/admin/customer-service/Workbench.vue'),
          meta: { title: '客服工作台' }
        },
        // 个人中心
        {
          path: 'profile',
          name: 'adminProfile',
          component: () => import('@/views/admin/profile/ProfileLayout.vue'),
          meta: { title: '个人中心' },
          redirect: '/admin/profile/info',
          children: [
            {
              path: 'info',
              name: 'adminProfileInfo',
              component: () => import('@/views/admin/profile/PersonalInfo.vue'),
              meta: { title: '个人信息管理' }
            },
            {
              path: 'password',
              name: 'adminProfilePassword',
              component: () => import('@/views/admin/profile/ChangePassword.vue'),
              meta: { title: '修改密码' }
            }
          ]
        }
      ]
    },
    // 租客端路由
    {
      path: '/tenant',
      component: () => import('@/views/tenant/TenantLayout.vue'),
      meta: { title: '租客首页', requiresAuth: true, userType: 1 },
      children: [
        {
          path: '',
          name: 'tenantDashboard',
          component: () => import('@/views/tenant/Dashboard.vue'),
          meta: { title: '首页' }
        },
        {
          path: 'search',
          name: 'tenantSearch',
          component: () => import('@/views/tenant/HouseSearch.vue'),
          meta: { title: '我要租房' }
        },
        {
          path: 'house/:houseId',
          name: 'tenantHouseDetail',
          component: () => import('@/views/tenant/HouseDetail.vue'),
          meta: { title: '房源详情' }
        },
        {
          path: 'payment',
          name: 'tenantPayment',
          component: () => import('@/views/tenant/Payment.vue'),
          meta: { title: '支付中心' }
        },
        {
          path: 'contract',
          name: 'tenantContract',
          component: () => import('@/views/tenant/Contract.vue'),
          meta: { title: '我的合同' }
        },
        {
          path: 'after',
          name: 'tenantAfter',
          component: () => import('@/views/tenant/After.vue'),
          meta: { title: '租后管理' }
        },
        {
          path: 'message',
          name: 'tenantMessage',
          component: () => import('@/views/tenant/Message.vue'),
          meta: { title: '消息通知' }
        },
        {
          path: 'statistics',
          name: 'tenantStatistics',
          component: () => import('@/views/tenant/Statistics.vue'),
          meta: { title: '支出分析' }
        },
        {
          path: 'calendar',
          name: 'tenantCalendar',
          component: () => import('@/views/tenant/Calendar.vue'),
          meta: { title: '租房日历' }
        },
        {
          path: 'chat',
          name: 'tenantChat',
          component: () => import('@/views/tenant/Chat.vue'),
          meta: { title: '聊天中心' }
        },
        {
          path: 'profile',
          name: 'tenantProfile',
          component: () => import('@/views/tenant/profile/ProfileLayout.vue'),
          meta: { title: '个人中心' },
          redirect: '/tenant/profile/realname',
          children: [
            {
              path: 'realname',
              name: 'tenantProfileRealname',
              component: () => import('@/views/tenant/profile/RealnameAuth.vue'),
              meta: { title: '实名认证' }
            },
            {
              path: 'info',
              name: 'tenantProfileInfo',
              component: () => import('@/views/tenant/profile/PersonalInfo.vue'),
              meta: { title: '个人信息管理' }
            },
            {
              path: 'card',
              name: 'tenantProfileCard',
              component: () => import('@/views/tenant/profile/PersonalCard.vue'),
              meta: { title: '个人名片' }
            },
            {
              path: 'credit',
              name: 'tenantProfileCredit',
              component: () => import('@/views/tenant/profile/CreditScore.vue'),
              meta: { title: '信用评分' }
            },
            {
              path: 'password',
              name: 'tenantProfilePassword',
              component: () => import('@/views/tenant/profile/ChangePassword.vue'),
              meta: { title: '修改密码' }
            }
          ]
        },
        {
          path: 'favorite',
          name: 'tenantFavorite',
          component: () => import('@/views/tenant/Favorite.vue'),
          meta: { title: '我的收藏' }
        }
      ]
    },
    // 房东端路由
    {
      path: '/landlord',
      component: () => import('@/views/landlord/LandlordLayout.vue'),
      meta: { title: '房东工作台', requiresAuth: true, userType: 2 },
      children: [
        {
          path: '',
          name: 'landlordDashboard',
          component: () => import('@/views/landlord/Dashboard.vue'),
          meta: { title: '工作台' }
        },
        {
          path: 'house',
          name: 'landlordHouse',
          redirect: '/landlord/house/list',
          meta: { title: '房源管理' }
        },
        {
          path: 'house/publish',
          name: 'landlordHousePublish',
          component: () => import('@/views/landlord/house/HousePublish.vue'),
          meta: { title: '发布房源', requiresRealname: true }
        },
        {
          path: 'house/list',
          name: 'landlordHouseList',
          component: () => import('@/views/landlord/house/HouseList.vue'),
          meta: { title: '房源列表', requiresRealname: true }
        },
        {
          path: 'rental',
          name: 'landlordRental',
          component: () => import('@/views/landlord/rental/RentalManage.vue'),
          meta: { title: '合同管理' }
        },
        {
          path: 'finance',
          name: 'landlordFinance',
          component: () => import('@/views/landlord/Finance.vue'),
          meta: { title: '财务管理' }
        },
        {
          path: 'after',
          name: 'landlordAfter',
          component: () => import('@/views/landlord/After.vue'),
          meta: { title: '租后服务' }
        },
        {
          path: 'statistics',
          name: 'landlordStatistics',
          component: () => import('@/views/landlord/Statistics.vue'),
          meta: { title: '收支分析' }
        },
        {
          path: 'contract-alert',
          name: 'landlordContractAlert',
          component: () => import('@/views/landlord/ContractAlert.vue'),
          meta: { title: '合同到期提醒' }
        },
        {
          path: 'message',
          name: 'landlordMessage',
          component: () => import('@/views/tenant/Message.vue'),
          meta: { title: '消息通知' }
        },
        {
          path: 'chat',
          name: 'landlordChat',
          component: () => import('@/views/landlord/Chat.vue'),
          meta: { title: '聊天中心' }
        },
        // 个人中心
        {
          path: 'profile',
          name: 'landlordProfile',
          component: () => import('@/views/landlord/profile/ProfileLayout.vue'),
          meta: { title: '个人中心' },
          redirect: '/landlord/profile/realname',
          children: [
            {
              path: 'realname',
              name: 'landlordProfileRealname',
              component: () => import('@/views/landlord/profile/RealnameAuth.vue'),
              meta: { title: '实名认证' }
            },
            {
              path: 'info',
              name: 'landlordProfileInfo',
              component: () => import('@/views/landlord/profile/PersonalInfo.vue'),
              meta: { title: '个人信息管理' }
            },
            {
              path: 'card',
              name: 'landlordProfileCard',
              component: () => import('@/views/landlord/profile/PersonalCard.vue'),
              meta: { title: '个人名片' }
            },
            {
              path: 'credit',
              name: 'landlordProfileCredit',
              component: () => import('@/views/landlord/profile/CreditScore.vue'),
              meta: { title: '信用评分' }
            },
            {
              path: 'password',
              name: 'landlordProfilePassword',
              component: () => import('@/views/landlord/profile/ChangePassword.vue'),
              meta: { title: '修改密码' }
            }
          ]
        }
      ]
    }
  ]
})

// 根据路由路径获取对应的token key
const getTokenKeyFromPath = (path: string): string => {
  if (path.startsWith('/tenant')) return 'token_tenant'
  if (path.startsWith('/landlord')) return 'token_landlord'
  if (path.startsWith('/admin')) return 'token_admin'
  return 'token'
}

// 路由守卫
router.beforeEach((to, from, next) => {
  // 根据目标路由获取对应的token
  const tokenKey = getTokenKeyFromPath(to.path)
  const token = localStorage.getItem(tokenKey)

  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router