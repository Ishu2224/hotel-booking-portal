import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    title: 'Padav | Hotel stays across India',
    loadComponent: () => import('./pages/search/search.page').then((m) => m.SearchPage),
  },
  {
    path: 'hotels/:id',
    title: 'Plan your stay | Padav',
    loadComponent: () => import('./pages/hotel-detail/hotel-detail.page').then((m) => m.HotelDetailPage),
  },
  {
    path: 'confirmation/:ref',
    title: 'Booking confirmed | Padav',
    loadComponent: () => import('./pages/confirmation/confirmation.page').then((m) => m.ConfirmationPage),
  },
  {
    path: 'manage',
    title: 'My booking | Padav',
    loadComponent: () => import('./pages/manage/manage.page').then((m) => m.ManagePage),
  },
  { path: '**', redirectTo: '' },
];
