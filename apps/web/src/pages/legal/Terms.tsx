import { Link } from "react-router";
import { LEGAL, contactText } from "../../lib/legal";
import { LegalPage, type LegalSection } from "./LegalPage";

const sections: LegalSection[] = [
  {
    id: "servico",
    title: "O serviço",
    body: (
      <>
        <p>
          A Carteirama é um sistema da suíte {LEGAL.company} para organizar clientes em grupos, registrar o valor e o vencimento de cada assinatura e acompanhar quanto você tem a
          receber no mês.
        </p>
        <p>
          A Carteirama <strong>não cobra os seus clientes</strong>, não emite notas fiscais nem movimenta dinheiro em seu nome. As cobranças que aparecem no sistema são registros
          para o seu controle; marcar uma cobrança como paga não confirma que o pagamento de fato aconteceu.
        </p>
        <p>Há duas formas de uso:</p>
        <ul>
          <li>
            <strong>Plano Cloud</strong>: rodamos o sistema na infraestrutura da {LEGAL.company}, com backups e suporte.
          </li>
          <li>
            <strong>Plano Comunitário</strong>: você instala o código aberto no seu próprio servidor. Nesse caso, estes termos valem apenas para o que for aplicável ao uso do código; a
            hospedagem, a segurança e os dados ficam sob a sua responsabilidade.
          </li>
        </ul>
      </>
    ),
  },
  {
    id: "conta",
    title: "Sua conta",
    body: (
      <>
        <p>Para usar o plano Cloud, você cria uma conta informando nome, e-mail, telefone e CPF ou CNPJ. Você se compromete a:</p>
        <ul>
          <li>informar dados verdadeiros e mantê-los atualizados;</li>
          <li>guardar sua senha em sigilo e avisar o suporte se suspeitar de acesso indevido;</li>
          <li>ser responsável por tudo o que for feito com o seu acesso.</li>
        </ul>
        <p>Cada CPF ou CNPJ pode ter uma única conta.</p>
      </>
    ),
  },
  {
    id: "teste-e-assinatura",
    title: "Teste grátis e assinatura",
    body: (
      <>
        <p>
          Toda conta nova começa com <strong>{LEGAL.trialDays} dias de teste grátis</strong>, sem cobrança. Para continuar depois do teste, é preciso assinar o plano Cloud. Se o teste
          terminar sem assinatura, a conta passa a ficar disponível só para consulta.
        </p>
        <p>
          O plano Cloud custa <strong>{LEGAL.cloudPrice} por mês</strong>. O valor que vale para você é o da data em que a sua assinatura foi criada; mudanças de preço valem para novas
          assinaturas, e qualquer reajuste da sua será avisado com antecedência.
        </p>
        <p>As faturas são mensais e podem ser pagas por cartão, Pix ou boleto, por meio do nosso provedor de pagamentos.</p>
      </>
    ),
  },
  {
    id: "atraso",
    title: "Atraso e suspensão",
    body: (
      <>
        <p>
          Se uma fatura ficar em aberto por mais de <strong>{LEGAL.suspendAfterDays} dias</strong> depois do vencimento, a conta é suspensa. Na conta suspensa você continua vendo e
          exportando os seus dados, mas não consegue cadastrar nem alterar nada.
        </p>
        <p>Assim que a fatura é paga, o acesso completo volta. Seus dados não são apagados por causa de uma suspensão.</p>
      </>
    ),
  },
  {
    id: "cancelamento",
    title: "Cancelamento",
    body: (
      <>
        <p>
          Você pode cancelar a assinatura a qualquer momento, na tela <em>Assinatura</em>. O acesso continua até o fim do período já pago, e não há multa nem cobrança proporcional.
        </p>
        <p>
          Depois do cancelamento, os dados da conta ficam guardados por <strong>{LEGAL.retentionDays} dias</strong> e, então, são apagados de forma definitiva. Use a exportação para
          planilha se quiser guardar uma cópia.
        </p>
        <p>Podemos encerrar contas que violem estes termos, com aviso prévio sempre que possível.</p>
      </>
    ),
  },
  {
    id: "dados-dos-clientes",
    title: "Dados dos seus clientes",
    body: (
      <>
        <p>
          Os dados que você cadastra sobre os seus clientes (nome, CPF ou CNPJ, e-mail, telefone, valores e observações) são seus. Perante a Lei Geral de Proteção de Dados (LGPD),
          você é o <strong>controlador</strong> desses dados e a {LEGAL.company} atua como <strong>operadora</strong>: tratamos essas informações apenas para fazer o sistema funcionar,
          seguindo as suas instruções.
        </p>
        <p>
          Cabe a você ter uma base legal para guardar os dados dos seus clientes e atender aos pedidos deles. Detalhes na nossa{" "}
          <Link to="/privacidade" className="font-semibold text-blue underline">
            política de privacidade
          </Link>
          .
        </p>
      </>
    ),
  },
  {
    id: "uso-permitido",
    title: "Uso permitido",
    body: (
      <>
        <p>Não é permitido usar a Carteirama para:</p>
        <ul>
          <li>guardar dados obtidos de forma ilícita ou sem base legal;</li>
          <li>tentar acessar contas ou dados de outras pessoas;</li>
          <li>sobrecarregar, testar falhas ou contornar limites do sistema sem autorização;</li>
          <li>qualquer atividade ilegal.</li>
        </ul>
      </>
    ),
  },
  {
    id: "disponibilidade",
    title: "Disponibilidade e backups",
    body: (
      <>
        <p>
          Fazemos backups diários e trabalhamos para manter o sistema no ar, mas não garantimos funcionamento ininterrupto. Pode haver paradas para manutenção ou por falhas de
          terceiros (como provedores de hospedagem e de pagamento).
        </p>
        <p>
          Na medida permitida pela lei, a {LEGAL.company} não responde por lucros cessantes ou prejuízos indiretos decorrentes do uso ou da indisponibilidade do sistema. Nossa
          responsabilidade total fica limitada ao valor pago por você nos últimos 12 meses.
        </p>
      </>
    ),
  },
  {
    id: "alteracoes",
    title: "Mudanças nestes termos",
    body: (
      <p>
        Podemos atualizar estes termos. Quando a mudança for relevante, avisamos por e-mail ou dentro do sistema antes de ela valer. Continuar usando a Carteirama depois disso significa
        que você concorda com a nova versão.
      </p>
    ),
  },
  {
    id: "lei-e-contato",
    title: "Lei aplicável e contato",
    body: (
      <>
        <p>Estes termos seguem a legislação brasileira, incluindo o Código de Defesa do Consumidor quando aplicável.</p>
        <p>Dúvidas sobre estes termos podem ser enviadas {contactText()}.</p>
      </>
    ),
  },
];

export default function Terms() {
  return (
    <LegalPage
      eyebrow="// Legal · termos"
      title="Termos de uso"
      updatedAt={LEGAL.updatedAt}
      other={{ to: "/privacidade", label: "Política de privacidade" }}
      sections={sections}
      intro={
        <p>
          Estes termos explicam as regras para usar a Carteirama, oferecida pela {LEGAL.company}. Ao criar uma conta, você declara que leu e concorda com eles e com a nossa{" "}
          <Link to="/privacidade" className="font-semibold text-blue underline">
            política de privacidade
          </Link>
          .
        </p>
      }
    />
  );
}
