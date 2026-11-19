import type { ComponentType } from "react";
import { createBrowserRouter } from "react-router";
import Landing from "./pages/Landing";
import NotFound from "./pages/NotFound";

/** Carrega a página sob demanda (o painel admin só é usado por poucos). */
const page = (load: () => Promise<{ default: ComponentType }>) => async () => ({ Component: (await load()).default });

const router = createBrowserRouter([
  { path: "/", element: <Landing /> },
  { path: "/termos", lazy: page(() => import("./pages/legal/Terms")) },
  { path: "/privacidade", lazy: page(() => import("./pages/legal/Privacy")) },
  { path: "*", element: <NotFound /> },
]);

export default router;
