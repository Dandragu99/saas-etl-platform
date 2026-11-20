export interface DemoClient {
  id: string;
  name: string;
  initials: string;
  contact: string;
  phone: string;
  address: string;
  activeWork: string;
  billing: number;
}
export interface DemoInvoice {
  id: string;
  number: string;
  client: string;
  date: string;
  due: string;
  amount: number;
  status: 'PAID' | 'PENDING' | 'OVERDUE';
  concept: string;
}
export interface DemoWork {
  id: string;
  title: string;
  client: string;
  location: string;
  status: 'PENDING' | 'ACTIVE' | 'DONE';
  progress: number;
  date: string;
}

export const DEMO_CLIENTS: readonly DemoClient[] = [
  {
    id: 'c1',
    name: 'Reformas Martínez',
    initials: 'RM',
    contact: 'Carlos Martínez',
    phone: '600 123 456',
    address: 'C/ Mayor 24, Madrid',
    activeWork: 'Instalación vivienda',
    billing: 4860,
  },
  {
    id: 'c2',
    name: 'Clínica Dental Nova',
    initials: 'CN',
    contact: 'Laura Santos',
    phone: '611 908 221',
    address: 'Av. Europa 18, Getafe',
    activeWork: 'Iluminación clínica',
    billing: 3250,
  },
  {
    id: 'c3',
    name: 'Restaurante La Brasa',
    initials: 'LB',
    contact: 'Miguel Ortega',
    phone: '644 330 782',
    address: 'C/ Prado 7, Madrid',
    activeWork: 'Revisión cuadro',
    billing: 1875,
  },
  {
    id: 'c4',
    name: 'Comunidad Jardines',
    initials: 'CJ',
    contact: 'Ana Ruiz',
    phone: '622 451 119',
    address: 'P.º Jardines 42, Leganés',
    activeWork: 'Zonas comunes',
    billing: 6120,
  },
  {
    id: 'c5',
    name: 'Estudio Norte',
    initials: 'EN',
    contact: 'Sara Vidal',
    phone: '633 772 042',
    address: 'C/ Fuencarral 91, Madrid',
    activeWork: 'Proyecto finalizado',
    billing: 2240,
  },
];

export const DEMO_INVOICES: readonly DemoInvoice[] = [
  {
    id: 'f1',
    number: 'FAC-2026-0042',
    client: 'Reformas Martínez',
    date: '18/11/2026',
    due: '18/12/2026',
    amount: 2486.55,
    status: 'PENDING',
    concept: 'Instalación eléctrica vivienda · Fase 1',
  },
  {
    id: 'f2',
    number: 'FAC-2026-0041',
    client: 'Clínica Dental Nova',
    date: '12/11/2026',
    due: '12/12/2026',
    amount: 1894.44,
    status: 'PAID',
    concept: 'Renovación iluminación LED',
  },
  {
    id: 'f3',
    number: 'FAC-2026-0038',
    client: 'Restaurante La Brasa',
    date: '21/10/2026',
    due: '20/11/2026',
    amount: 968.0,
    status: 'OVERDUE',
    concept: 'Cuadro eléctrico y protecciones',
  },
  {
    id: 'f4',
    number: 'FAC-2026-0035',
    client: 'Comunidad Jardines',
    date: '05/10/2026',
    due: '04/11/2026',
    amount: 3420.67,
    status: 'PENDING',
    concept: 'Alumbrado zonas comunes',
  },
];

export const DEMO_WORKS: readonly DemoWork[] = [
  {
    id: 'o1',
    title: 'Instalación vivienda',
    client: 'Reformas Martínez',
    location: 'C/ Alcalá 214',
    status: 'ACTIVE',
    progress: 68,
    date: 'Hoy · 09:30',
  },
  {
    id: 'o2',
    title: 'Iluminación clínica',
    client: 'Clínica Dental Nova',
    location: 'Av. Europa 18',
    status: 'ACTIVE',
    progress: 35,
    date: 'Mañana · 08:00',
  },
  {
    id: 'o3',
    title: 'Revisión cuadro',
    client: 'Restaurante La Brasa',
    location: 'C/ Prado 7',
    status: 'PENDING',
    progress: 0,
    date: '23 nov · 10:00',
  },
  {
    id: 'o4',
    title: 'Alumbrado zonas comunes',
    client: 'Comunidad Jardines',
    location: 'P.º Jardines 42',
    status: 'PENDING',
    progress: 12,
    date: '25 nov · 08:30',
  },
  {
    id: 'o5',
    title: 'Domótica despacho',
    client: 'Estudio Norte',
    location: 'C/ Fuencarral 91',
    status: 'DONE',
    progress: 100,
    date: 'Finalizada 14 nov',
  },
];

export const money = (value: number): string =>
  new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' }).format(value);
