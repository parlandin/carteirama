import type { ComponentType } from "react";
import { createBrowserRouter } from "react-router";
import { AdminLayout, AppLayout, GuestOnly } from "./layouts/AppLayout";
import Landing from "./pages/Landing";
import NotFound from "./pages/NotFound";
import Account from "./pages/app/Account";
import Charges from "./pages/app/Charges";
import ClientDetail from "./pages/app/ClientDetail";
import ClientForm from "./pages/app/ClientForm";
import Clients from "./pages/app/Clients";
import Export from "./pages/app/Export";
import GroupDetail from "./pages/app/GroupDetail";
import GroupForm from "./pages/app/GroupForm";
import Groups from "./pages/app/Groups";
import Subscription from "./pages/app/Subscription";
import Login from "./pages/auth/Login";
import { ForgotPassword, ResetPassword } from "./pages/auth/PasswordPages";
import Register from "./pages/auth/Register";

const page = (load: () => Promise<{ default: ComponentType }>) => async () => ({ Component: (await load()).default });

const router = createBrowserRouter([
  { path: "/", element: <Landing /> },
  { path: "/termos", lazy: page(() => import("./pages/legal/Terms")) },
  { path: "/privacidade", lazy: page(() => import("./pages/legal/Privacy")) },
  {
    element: <GuestOnly />,
    children: [
      { path: "/entrar", element: <Login /> },
      { path: "/criar-conta", element: <Register /> },
      { path: "/esqueci-a-senha", element: <ForgotPassword /> },
    ],
  },
  { path: "/reset-password", element: <ResetPassword /> },
  {
    path: "/app",
    element: <AppLayout />,
    children: [
      { index: true, element: <Groups /> },
      { path: "grupos/novo", element: <GroupForm /> },
      { path: "grupos/:id", element: <GroupDetail /> },
      { path: "grupos/:id/editar", element: <GroupForm /> },
      { path: "clientes", element: <Clients /> },
      { path: "clientes/novo", element: <ClientForm /> },
      { path: "clientes/:id", element: <ClientDetail /> },
      { path: "clientes/:id/editar", element: <ClientForm /> },
      { path: "cobrancas", element: <Charges /> },
      { path: "exportar", element: <Export /> },
      { path: "assinatura", element: <Subscription /> },
      { path: "assinatura/faturas/:id", lazy: page(() => import("./pages/app/InvoicePayment")) },
      { path: "conta", element: <Account /> },
    ],
  },
  {
    path: "/admin",
    element: <AdminLayout />,
    children: [
      { index: true, lazy: page(() => import("./pages/admin/Overview")) },
      { path: "contas", lazy: page(() => import("./pages/admin/Accounts")) },
      { path: "contas/:id", lazy: page(() => import("./pages/admin/AccountDetail")) },
      { path: "pagamentos", lazy: page(() => import("./pages/admin/Payments")) },
      { path: "planos", lazy: page(() => import("./pages/admin/Plans")) },
    ],
  },
  { path: "*", element: <NotFound /> },
]);

export default router;
