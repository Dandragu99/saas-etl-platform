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
        path: 'inventario',
        title: 'Inventario | Nexo ERP',
        data: { pageTitle: 'Inventario' },
        loadComponent: () => import('./inventory/inventory').then((module) => module.Inventory),
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
      path: 'clientes',
      title: 'Clientes',
      description: 'Centraliza la información y la actividad de tus clientes.',
    },
    {
      path: 'ventas',
      title: 'Ventas',
      description: 'Gestiona presupuestos, pedidos y facturación.',
    },
    {
      path: 'gastos',
      title: 'Gastos',
      description: 'Registra y clasifica los gastos de tu empresa.',
    },
    {
      path: 'asistente-ia',
      title: 'Asistente IA',
      description: 'Obtén ayuda contextual para tus tareas de gestión.',
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
