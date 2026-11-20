import { Routes } from '@angular/router';

import { MainLayout } from './layout/main-layout';
import { authGuard } from './auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    title: 'Iniciar sesión | Nexo ERP',
    loadComponent: () => import('./auth/login/login').then((module) => module.Login),
  },
  {
    path: 'registro',
    title: 'Crear cuenta | Nexo ERP',
    loadComponent: () => import('./auth/register/register').then((module) => module.Register),
  },
  {
    path: '',
    component: MainLayout,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        title: 'Dashboard | Nexo ERP',
        data: { pageTitle: 'Dashboard' },
        loadComponent: () => import('./dashboard/dashboard').then((module) => module.Dashboard),
      },
      {
        path: 'transformaciones',
        title: 'Transformaciones | Nexo ERP',
        data: { pageTitle: 'Transformaciones' },
        loadComponent: () =>
          import('./csv-preview/csv-preview').then((module) => module.CsvPreview),
      },
      {
        path: 'productos',
        title: 'Productos | Nexo ERP',
        data: { pageTitle: 'Productos' },
        loadComponent: () => import('./products/products').then((module) => module.Products),
      },
      {
        path: 'inventario-operativo',
        title: 'Inventario operativo | Nexo ERP',
        data: { pageTitle: 'Inventario operativo' },
        loadComponent: () => import('./inventory/inventory').then((module) => module.Inventory),
      },
      {
        path: 'gestion',
        title: 'Clientes y ventas | Nexo ERP',
        data: { pageTitle: 'Clientes y ventas' },
        loadComponent: () =>
          import('./business/business-workspace').then((module) => module.BusinessWorkspace),
      },
      {
        path: 'clientes',
        title: 'Clientes | Voltia',
        data: { pageTitle: 'Clientes' },
        loadComponent: () => import('./demo/clients-page').then((module) => module.ClientsPage),
      },
      {
        path: 'obras',
        title: 'Obras | Voltia',
        data: { pageTitle: 'Obras' },
        loadComponent: () => import('./demo/works-page').then((module) => module.WorksPage),
      },
      {
        path: 'facturas',
        title: 'Facturas | Voltia',
        data: { pageTitle: 'Facturas' },
        loadComponent: () => import('./demo/invoices-page').then((module) => module.InvoicesPage),
      },
      { path: 'presupuestos', pathMatch: 'full', redirectTo: 'gestion' },
      { path: 'ventas', pathMatch: 'full', redirectTo: 'facturas' },
      {
        path: 'inventario',
        title: 'Inventario | Voltia',
        data: { pageTitle: 'Inventario' },
        loadComponent: () => import('./demo/inventory-page').then((module) => module.InventoryPage),
      },
      {
        path: 'asistente-ia',
        title: 'Asistente | Voltia',
        data: { pageTitle: 'Asistente IA' },
        loadComponent: () => import('./demo/assistant-page').then((module) => module.AssistantPage),
      },
      ...comingSoonRoutes(),
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];

function comingSoonRoutes(): Routes {
  const modules = [
    {
      path: 'documentos',
      title: 'Documentos',
      description: 'Organiza y consulta los documentos de tu negocio desde un único lugar.',
    },
    {
      path: 'gastos',
      title: 'Gastos',
      description: 'Registra y clasifica los gastos de tu empresa.',
    },
  ] as const;

  return modules.map((module) => ({
    path: module.path,
    title: `${module.title} | Nexo ERP`,
    data: {
      pageTitle: module.title,
      moduleTitle: module.title,
      moduleDescription: module.description,
    },
    loadComponent: () =>
      import('./coming-soon/coming-soon').then((loadedModule) => loadedModule.ComingSoon),
  }));
}
