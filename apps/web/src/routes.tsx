import type { ComponentType } from "react";
import { createBrowserRouter } from "react-router";
import { AdminLayout, AppLayout, GuestOnly } from "./layouts/AppLayout";
import Landing from "./pages/Landing";
import NotFound from "./pages/NotFound";
import GroupDetail from "./pages/app/GroupDetail";
import GroupForm from "./pages/app/GroupForm";
import Groups from "./pages/app/Groups";
import Login from "./pages/auth/Login";
import { ForgotPassword, ResetPassword } from "./pages/auth/PasswordPages";
import Register from "./pages/auth/Register";

/** Carrega a página sob demanda (o painel admin só é usado por poucos). */
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
  // O link do e-mail de redefinição aponta para este caminho (AuthService na API).
  { path: "/reset-password", element: <ResetPassword /> },
  {
    path: "/app",
    element: <AppLayout />,
    children: [
      { index: true, element: <Groups /> },
      { path: "grupos/novo", element: <GroupForm /> },
      { path: "grupos/:id", element: <GroupDetail /> },
      { path: "grupos/:id/editar", element: <GroupForm /> },
    ],
  },
  {
    path: "/admin",
    element: <AdminLayout />,
    children: [
    ],
  },
  { path: "*", element: <NotFound /> },
]);

export default router;
