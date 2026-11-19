import { Link } from "react-router";
import { LEGAL, contactText } from "../../lib/legal";
import { LegalPage, type LegalSection } from "./LegalPage";

const sections: LegalSection[] = [
  {
    id: "papeis",
    title: "Quem trata os dados",
    body: (
      <>
        <p>
          A {LEGAL.company} é a <strong>controladora</strong> dos dados da sua
          conta (os seus dados como usuário e assinante da Carteirama).
        </p>
        <p>
          Para os dados que você cadastra sobre os{" "}
          <strong>seus clientes</strong>, a {LEGAL.company} é{" "}
          <strong>operadora</strong>: tratamos esses dados só para o sistema
          funcionar e seguindo as suas instruções. Quem decide o que cadastrar,
          e responde aos seus clientes sobre isso, é você.
        </p>
      </>
    ),
  },
  {
    id: "dados-coletados",
    title: "Dados que coletamos",
    body: (
      <>
        <p>
          <strong>Dados da conta:</strong> nome, e-mail, telefone, CPF ou CNPJ,
          razão social, nome fantasia e, se você informar, endereço, cidade,
          estado e CEP.
        </p>
        <p>
          <strong>Senha:</strong> guardada apenas como hash criptográfico. Nem
          nós conseguimos ver a sua senha.
        </p>
        <p>
          <strong>Dados de assinatura:</strong> plano, valor, faturas, datas e
          forma de pagamento. Os dados do cartão são digitados e guardados pelo
          provedor de pagamentos; a Carteirama não armazena número de cartão.
        </p>
        <p>
          <strong>Dados técnicos:</strong> endereço IP e registros de acesso
          (data, hora e rota acessada), usados para segurança, limite de
          tentativas de login e investigação de erros.
        </p>
      </>
    ),
  },
  {
    id: "finalidades",
    title: "Para que usamos",
    body: (
      <ul>
        <li>criar e manter a sua conta e permitir o login;</li>
        <li>mostrar os seus grupos, clientes, cobranças e totais;</li>
        <li>
          emitir e cobrar as faturas da assinatura e enviar recibos e avisos de
          vencimento;
        </li>
        <li>enviar e-mails de serviço, como redefinição de senha;</li>
        <li>proteger o sistema contra abuso e acessos indevidos;</li>
        <li>cumprir obrigações legais, como as fiscais.</li>
      </ul>
    ),
  },
  {
    id: "bases-legais",
    title: "Bases legais (LGPD)",
    body: (
      <ul>
        <li>
          <strong>Execução de contrato</strong>: para prestar o serviço que você
          contratou;
        </li>
        <li>
          <strong>Cumprimento de obrigação legal</strong>: quando a lei exigir
          que algum dado seja tratado ou informado;
        </li>
        <li>
          <strong>Legítimo interesse</strong>: para segurança, prevenção de
          fraude e melhoria do serviço, sempre respeitando os seus direitos.
        </li>
      </ul>
    ),
  },
  {
    id: "compartilhamento",
    title: "Com quem compartilhamos",
    body: (
      <>
        <p>
          Não vendemos nem alugamos dados. Compartilhamos apenas o necessário
          com:
        </p>
        <ul>
          <li>
            o provedor de pagamentos, para processar as faturas da assinatura;
          </li>
          <li>o provedor de envio de e-mails, para os e-mails de serviço;</li>
          <li>a infraestrutura de hospedagem onde o sistema roda;</li>
          <li>autoridades, quando houver obrigação legal ou ordem judicial.</li>
        </ul>
      </>
    ),
  },
  {
    id: "cookies",
    title: "Cookies",
    body: (
      <>
        <p>
          Usamos um único cookie próprio, que mantém você conectado. Ele é
          necessário para o login, não é acessível por scripts da página e não
          serve para rastrear você em outros sites. Não usamos cookies de
          publicidade.
        </p>
        <p>
          As fontes do site são carregadas do Google Fonts; ao carregá-las, o
          seu navegador se comunica com os servidores do Google.
        </p>
      </>
    ),
  },
  {
    id: "retencao",
    title: "Por quanto tempo guardamos",
    body: (
      <>
        <p>Guardamos os dados enquanto a sua conta existir.</p>
        <p>
          Se você cancelar a assinatura, os dados da conta, dos seus clientes,
          das cobranças e das faturas são apagados de forma definitiva{" "}
          <strong>{LEGAL.retentionDays} dias</strong> após o cancelamento.
        </p>
        <p>
          Links de redefinição de senha expiram e são apagados automaticamente.
        </p>
      </>
    ),
  },
  {
    id: "seguranca",
    title: "Segurança",
    body: (
      <ul>
        <li>conexão criptografada (HTTPS) em todo o sistema;</li>
        <li>senhas guardadas com hash2;</li>
        <li>cookie de sessão protegido contra acesso por scripts;</li>
        <li>limite de tentativas de login e de requisições;</li>
        <li>cada conta só acessa os próprios dados;</li>
        <li>backups diários.</li>
      </ul>
    ),
  },
  {
    id: "direitos",
    title: "Seus direitos",
    body: (
      <>
        <p>Pela LGPD, você pode pedir a qualquer momento:</p>
        <ul>
          <li>confirmação de que tratamos seus dados e acesso a eles;</li>
          <li>
            correção de dados incompletos ou desatualizados (boa parte você
            mesmo altera em <em>Meus dados</em>);
          </li>
          <li>anonimização, bloqueio ou eliminação de dados desnecessários;</li>
          <li>
            portabilidade (a exportação para planilha já entrega seus dados);
          </li>
          <li>informação sobre com quem compartilhamos;</li>
          <li>revisão desta política e reclamação à ANPD.</li>
        </ul>
        <p>
          Se um cliente seu pedir algo sobre os dados dele, o pedido deve ser
          feito a você, que é o controlador. Podemos ajudar a atendê-lo.
        </p>
      </>
    ),
  },
  {
    id: "contato",
    title: "Contato e mudanças",
    body: (
      <>
        <p>
          Pedidos e dúvidas sobre privacidade podem ser enviados {contactText()}
          .
        </p>
        <p>
          Podemos atualizar esta política. Quando a mudança for relevante,
          avisamos por e-mail ou dentro do sistema. Veja também os{" "}
          <Link to="/termos" className="font-semibold text-blue underline">
            termos de uso
          </Link>
          .
        </p>
      </>
    ),
  },
];

export default function Privacy() {
  return (
    <LegalPage
      eyebrow="// Legal · privacidade"
      title="Política de privacidade"
      updatedAt={LEGAL.updatedAt}
      other={{ to: "/termos", label: "Termos de uso" }}
      sections={sections}
      intro={
        <p>
          Esta política explica quais dados a Carteirama coleta, para que usa,
          com quem compartilha e como você exerce os seus direitos, de acordo
          com a Lei Geral de Proteção de Dados (Lei nº 13.709/2018).
        </p>
      }
    />
  );
}
