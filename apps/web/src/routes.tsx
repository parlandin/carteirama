import type { ComponentType } from "react";
import { createBrowserRouter } from "react-router";
import Home from "./pages/home";

/** Carrega a página sob demanda (o painel admin só é usado por poucos). */
const page = (load: () => Promise<{ default: ComponentType }>) => async () => ({ Component: (await load()).default });

const router = createBrowserRouter([
  { path: "/", index: true, element: <Home /> },
  { path: "/termos", lazy: page(() => import("./pages/legal/Terms")) },
  { path: "/privacidade", lazy: page(() => import("./pages/legal/Privacy")) },
]);

export default router;
